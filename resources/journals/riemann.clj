(require '[riemann.client :as riemann])
(use 'overtone.at-at)

(def ^{:private true} client-config
  (or (get-config :riemann) {:host "localhost"}))

(case (get client-config :protocol)
    "udp" (def  ^{:private true} client (riemann/udp-client client-config))
    (def  ^{:private true} client (riemann/tcp-client client-config)))

(def ^{:private true} publication-responses (atom []))

(def riemann-default-tags ["bohr"])

(defn- riemann-tags []
  (get client-config :tags riemann-default-tags))

(defn- event-from-observation [name value options]
  (let [riemann-attributes
        (if (:units options)
          (assoc (get options :attrs {}) :units (:units options))
          (get options :attrs {}))]
  (assoc
   {:service     name
    :description (get options :desc)
    :attributes  riemann-attributes
    :tags        (riemann-tags)
    }
   (if (number? value) :metric :state)
   (if (number? value) value   (str value)))))

(defn riemann-journal [name value options]
  (let [event (event-from-observation name value options)]
    (log/trace "Publishing to Riemann:" event)
    (swap!
     publication-responses
     conj
     (riemann/send-event
      client
      event))))

(define-journal! "riemann" riemann-journal)

(def response-handler-period 1000)
(def response-handler-pool (mk-pool))

(defn- handle-responses! []
  (while (not (empty? @publication-responses))
    (let [response-ref (first @publication-responses)
          response     (deref response-ref  5000 ::timeout)
          response-error (get response :error)]
      (log/trace "Handling response from Riemann" response)
      (if response-error
        (log/error "Could not publish to Riemann:" response-error)))
    (swap! publication-responses subvec 1)))

(defn- setup-response-handler! []
  (log/debug "Listening for responses from Riemann every" response-handler-period "ms")
  (let [schedule
        (every
         response-handler-period
         #(handle-responses!)
         response-handler-pool)]
    schedule))

(setup-response-handler!)
