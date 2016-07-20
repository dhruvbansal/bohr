(def disk-annotations
  {
   :reads            { :desc "Number of reads completed" :attrs { :agg "last" :counter true }}
   :reads-merged     { :desc "Number of reads merged" :attrs { :agg "last" :counter true }}
   :data.read        { :desc "Data read" :units "B" :attrs { :agg "last" :counter true }}
   :time.read        { :desc "Time spent reading" :units "ms" :attrs { :agg "last" :counter true }}
   :writes           { :desc "Number of writes completed" :attrs { :agg "last" :counter true }}
   :writes-merged    { :desc "Number of writes merged" :attrs { :agg "last" :counter true }}
   :data.written     { :desc "Data written" :units "B" :attrs { :agg "last" :counter true }}
   :time.write       { :desc "Time spent writing" :units "ms" :attrs { :agg "last" :counter true }}
   :current-io       { :desc "Number of current IOs" :attrs { :agg "mean" }}
   :time.io          { :desc "Time spent on IO" :units "ms" :attrs { :agg "last" :counter true }}
   :time.io-weighted { :desc "Weighted IO time" :units "ms" :attrs { :agg "last" :counter true }}
   })

;; FIXME -- sector size should *ideally* be determined at runtime by
;; introspecting on the device's physical/logical sector size (e.g. -
;; 'cat /sys/block/DEVICE_NAME/queue/hw_sector_size') but the
;; *logical* sector size is almost always 512, even on modern drives
;; with 4K physical sectors.
(def sector-size 512)
  
(defn- disks-linux []
  (map-table
   :name
   (parse-table
    (procfile-contents "diskstats")
    [[nil               identity]
     [nil               identity]
     [:name             identity]
     [:reads            :long]
     [:reads-merged     :long]
     [:data.read        sector-size]
     [:time.read        :long]
     [:writes           :long]
     [:writes-merged    :long]
     [:data.written     sector-size]
     [:time.write       :long]
     [:current-io       :long]
     [:time.io          :long]
     [:time.io-weighted :long]]
    :row-filter
    #(not (re-find #"(loop|ram)" (get % :name))))
   :delete-key true
   :transform-row #(annotate % disk-annotations)))

(defn- disks []
  (case-os
   "Linux" (disks-linux)))

(observe :disk :period 10 :prefix "disk"
         (doseq [[disk-name disk] (seq (disks))]
           (submit-many disk :attrs { :device (str "/dev/" disk-name) })))
