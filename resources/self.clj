;;; self.clj
;;;
;;; Defines observers for Bohr itself.

(static  :bohr.start-time (time/now))

(observe :bohr :ttl 5 :prefix "bohr" :tags ["bohr"]
         (do
           (submit "runtime"
                   (time/interval (:bohr.start-time) (time/now)))
           (submit "observers"           (observer-count))
           (submit "observations"        @observations)
           (submit "metrics.gathered"    @submissions)
           (submit "metrics.transmitted" @publications)))
