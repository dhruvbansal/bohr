(require '[clj-time.core :as time])

(static :start-time (time/now))

(observe :runtime :ttl 5 (time/interval (:start-time) (time/now)))

(observe :loop-test :ttl 5 :tags ["system", "boot"]
        (submit "runtime" (time/in-seconds (:runtime)) :desc "Runtime" :units "s"))
