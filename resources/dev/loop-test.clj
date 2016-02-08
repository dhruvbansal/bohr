(require '[clj-time.core :as time])

(static :start-time (time/now))

(observe :loop-test :ttl 5 :tags ["system", "boot"]
        (submit "runtime" (time/in-seconds (time/interval (:start-time) (time/now))) :desc "Runtime" :units "s"))
