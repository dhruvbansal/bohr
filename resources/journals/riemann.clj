(require '[riemann.client :as riemann])
(use 'overtone.at-at)

(def ^{:private true} client-config
  (or (get-config :riemann) {:host "localhost"}))

(case (get client-config :protocol)
    "udp" (def  ^{:private true} client (riemann/udp-client client-config))
    (def  ^{:private true} client (riemann/tcp-client client-config)))

(def ^{:private true} publication-responses (atom []))

(defn- event-from-observation [name value options]
  (let [event
        {:service     name
         :tags        (get options :tags [])
         :description (get options :desc)
         :attributes  (get options :attributes {})
         }

        event-with-units
        (if (get options :units)
          (assoc event :attributes
                 (assoc (get :attributes event) :units (get options :units)))
          event)

        event-with-value
        (if (number? value)
          (assoc event-with-units :metric value)
          (assoc event-with-units :state (str value)))]
    event-with-value))

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
