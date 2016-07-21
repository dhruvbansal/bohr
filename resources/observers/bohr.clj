;;; self.clj
;;;
;;; Defines observers for Bohr itself.

(def bohr-start-time (time/now))

(defn bohr-runtime []
  (time/in-seconds
   (time/interval bohr-start-time (time/now))))

(observe :bohr :period 5 :prefix "bohr"
         (do
           (submit "status"              "OK", :desc "Current Bohr status" :attrs { :agg "last" })
           (submit "runtime"             (bohr-runtime) :units "s" :desc "Bohr uptime" :attrs { :agg "last" })
           (submit "observers"           (observer-count) :desc "Number of defined observers" :attrs {:agg "mean" })
           (submit "observations"        @observations :desc "Number of observations made" :attrs { :agg "last" :counter true })
           (submit "metrics.gathered"    @submissions :desc "Number of unique metrics gathered" :attrs { :agg "last" :counter true })
           (submit "metrics.transmitted" @publications :desc "Number of metrics transmitted via all journals" :attrs { :agg "last" :counter true })))
