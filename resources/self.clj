;;; self.clj
;;;
;;; Defines observers for Bohr itself.

(static :bohr.start-time (time/now))

(defn bohr-runtime [start-time]
  (time/in-seconds
   (time/interval start-time (time/now))))

(observe :bohr :ttl 5 :prefix "bohr" :tags ["bohr"]
         (do
           (submit "runtime"             (bohr-runtime (& :bohr.start-time)) :units "s")
           (submit "observers"           (observer-count))
           (submit "observations"        @observations)
           (submit "metrics.gathered"    @submissions)
           (submit "metrics.transmitted" @publications)))
