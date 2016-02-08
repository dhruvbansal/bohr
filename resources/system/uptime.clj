(static :uptime.regexp #"(\d+) +days?, +(\d+):(\d+)")

(observe :uptime.output :ttl 5
         (string/trim (get (sh "uptime") :out)))

(observe :uptime.seconds
         (let [[_ days hours minutes]
               (re-find (:uptime.regexp)  (:uptime.output))]
           (+
            (* (Integer/parseInt days)    86400)
            (* (Integer/parseInt hours)   3600)
            (* (Integer/parseInt minutes) 60))))

(observe :uptime :ttl 5 :tags ["system", "boot"]
        (submit "uptime" (:uptime.seconds) :desc "Uptime" :units "s"))
