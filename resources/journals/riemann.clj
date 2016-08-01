;;;; riemann.clj
;;;;
;;;; Defines a journal which writes events to Riemann and an observer
;;;; which reports metrics about this journal.
;;;;
;;;; The journal uses Riemann's TCP protocol to ensure that events are
;;;; not dropped.  Requests to Riemann are sent without blocking and
;;;; their responses are handled in a separate thread.

(require '[riemann.client :as riemann])
(use 'overtone.at-at)

(def ^{:private true} client-config
  (or (get-config :riemann) {:host "localhost"}))

(def ^{:private true} request-count (atom 0))
(def ^{:private true} response-count (atom 0))
(def ^{:private true} successful-response-count (atom 0))
(def ^{:private true} failed-response-count (atom 0))

(def ^{:private true} responses (atom []))

(def riemann-default-tags ["bohr"])

(def ^{:private true} client (atom nil))
(def ^{:private true} client-refresh-period 10)
(def ^{:private true} client-refresh-pool (mk-pool))
(def ^{:private true} client-refresh-schedule (atom nil))

(defn- refresh-riemann-client!
  "Set a new Riemann client."
  []
  (log/trace "Refreshing Riemann client")
  (reset!
   client
   (case (get client-config :protocol)
     "udp" (riemann/udp-client client-config)
     (riemann/tcp-client client-config))))
(refresh-riemann-client!)

(defn- setup-client-refresher! []
  (log/debug "Refreshing Riemann connection every" client-refresh-period "s")
  (reset!
   client-refresh-schedule
   (every
    (* client-refresh-period 1000)       ; s -> ms
    refresh-riemann-client!
    client-refresh-pool)))

;(setup-client-refresher!)
   
(defn- riemann-tags []
  (get client-config :tags riemann-default-tags))

(defn- event-from-observation [name value options]
  (assoc
   (merge
    (if (:units options)
      (assoc (get options :attrs {}) :units (:units options))
      (get options :attrs {}))
    {:service     name
     :description (get options :desc)
     :tags        (riemann-tags)
     :ttl         (let [ttl (get-observer current-observer :period)]
                    (if ttl (* 3 ttl))) ; some  buffer
     })
   (if (number? value) :metric :state)
   (if (number? value) value   (str value))))

(defn riemann-journal [name value options]
  (let [event (event-from-observation name value options)]
    (log/trace "Publishing to Riemann:" event)
    (swap!
     responses
     conj
     (riemann/send-event
      @client
      event))
    (swap!
     request-count
     inc)))

(define-journal! "riemann" riemann-journal)

(def response-handler-period 1000)
(def response-handler-pool (mk-pool))
(def response-handler-schedule (atom nil))

(defn- handle-responses! []
  (while (not (empty? @responses))
    (let [response-ref (first @responses)
          response     (deref response-ref  5000 ::timeout)
          response-error (get response :error)]
      (log/trace "Handling response from Riemann" response)
      (swap! response-count inc)
      (if response-error
        (do
          (log/error "Could not publish to Riemann:" response-error)
          (swap! failed-response-count inc))
        (swap! successful-response-count inc)))
    (swap! responses subvec 1)))

(defn- handled-all-responses? []
  (empty? @responses))

(defn- setup-response-handler! []
  (log/debug "Listening for responses from Riemann every" response-handler-period "ms")
  (reset!
   response-handler-schedule
   (every
    response-handler-period
    #(handle-responses!)
    response-handler-pool))
  (schedule! "riemann-response-handler" handled-all-responses?))

(setup-response-handler!)

(observe :journal-riemann :period 5 :prefix "bohr.journals.riemann"
         (do
           (submit "requests" @request-count :desc "Number of events sent to Riemann" :attrs { :agg "last" :counter true })
           (submit "requests.lag" (- @request-count @response-count) :desc "Lag between requests and responses " :attrs { :agg "mean"})
           (submit "responses.ok" @successful-response-count :desc "Number of successful responses received from Riemann" :attrs { :agg "last" :counter true })
           (submit "responses.fail" @failed-response-count :desc "Number of failed responses received from Riemann" :attrs { :agg "last" :counter true })))
