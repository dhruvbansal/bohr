(require '[clojure.java.shell :refer [sh]])
(require '[clojure.string :as string])

(define-observer! :uptime.regexp  {}
  (fn [] #"(\d+) +days?, +(\d+):(\d+)")
  [])

(define-observer! :uptime.output  {:ttl 5}
  #(string/trim (get (sh "uptime") :out))
  [])

(define-observer! :uptime.seconds {}
  #(let [[_ days hours minutes]
         (re-find (force-get-reading! :uptime.regexp)  (force-get-reading! :uptime.output))]
          (+
           (* (Integer/parseInt days)    86400)
           (* (Integer/parseInt hours)   3600)
           (* (Integer/parseInt minutes) 60)))
  [:uptime.regexp :uptime.output])

(define-observer! :uptime {:ttl 5 :desc "Uptime" :tags ["system", "boot"] }
  #(submit-in-observer! :uptime "uptime" (force-get-reading! :uptime.seconds) :units "s")
  [:uptime.seconds])
