(require '[clojure.java.shell :refer [sh]])
(require '[clojure.string :as string])
(require '[clj-time.core :as time])

(static :uptime.regexp      #"(\d+) +days?, +(\d+):(\d+)")
(static :uptime.start-time  (time/now))

(measure :uptime.output :ttl 5
         (string/trim (get (sh "uptime") :out)))

;; (calc :uptime.seconds
;;       ([[_ days hours minutes]
;;         (re-find (:uptime.regexp)  (:uptime.output))]
;;        (+
;;         (* (Integer/parseInt days)    86400)
;;         (* (Integer/parseInt hours)   3600)
;;         (* (Integer/parseInt minutes) 60))))

;; (report :uptime :ttl 10 :desc "Uptime" :tags ["system", "boot"]
;;         (submit :report "uptime" (:uptime.seconds) :units "s"))
