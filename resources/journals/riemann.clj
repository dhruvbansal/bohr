;; (require '[riemann.client :as riemann])

;; (def client (riemann/tcp-client {:host "127.0.0.1"}))
;; (-> c (r/send-event {:service "foo" :state "ok"})
;;       (deref 5000 ::timeout))
;; @(r/query c "state = \"ok\"")

;; (println "HI")
