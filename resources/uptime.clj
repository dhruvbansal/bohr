(println "Unmodififed")
(def lexvar "lexvar value")
(require [clojure.java.shell :refer [sh]])

(observe :var.lexical.direct    (format "lexical.direct: %s" lexvar))
(observe :var.lexical.deferred ((format "lexical.deferred: %s" lexvar)))

(set :uptime.regexp #"(\d+) +days?, +(\d+):(\d+)")

(measure :uptime.output :ttl 10
         (get (sh "uptime") :out))

(calc :uptime.seconds
         ([[_ days hours minutes]
           (re-find (:uptime.regexp)  (:uptime.output))]
          (+
           (* (Integer/parseInt days)    86400)
           (* (Integer/parseInt hours)   3600)
           (* (Integer/parseInt minutes) 60))))

(report :uptime :ttl 10 :desc "Uptime" :tags ("system", "boot")
         ((metric "uptime" (:uptime.seconds) :units "s")))
