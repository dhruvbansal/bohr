(def disk-annotations
  {
   :reads            { :desc "Number of reads completed" :tags ["counter"]}
   :reads-merged     { :desc "Number of reads merged" :tags ["counter"]}
   :bytes.read       { :desc "Number of bytes read" :units "B" :tags ["counter"]}
   :time.read        { :desc "Time spent reading" :units "ms" :tags ["counter"]}
   :writes           { :desc "Number of writes completed" :tags ["counter"]}
   :writes-merged    { :desc "Number of writes merged" :tags ["counter"]}
   :bytes.written    { :desc "Number of bytes written" :units "B" :tags ["counter"]}
   :time.write       { :desc "Time spent writing" :units "ms" :tags ["counter"]}
   :current-io       { :desc "Number of current IOs"}
   :time.io          { :desc "Time spent on IO" :units "ms" :tags ["counter"]}
   :time.io-weighted { :desc "Weighted IO time" :units "ms" :tags ["counter"]}
   })

;; FIXME -- sector size should *ideally* be determined at runtime by
;; introspecting on the device's physical/logical sector size (e.g. -
;; 'cat /sys/block/DEVICE_NAME/queue/hw_sector_size') but the
;; *logical* sector size is almost always 512, even on modern drives
;; with 4K physical sectors.
(def sector-size 512)
(defn- sectors-to-bytes [sectors]
  (* sector-size sectors))
  
(defn- disks-linux []
  (map-table
   :name
   (parse-table
    (procfile-contents "diskstats")
    [[nil              identity]
     [nil              identity]
     [:name            identity]
     [:reads            #(Long/parseLong %)]
     [:reads-merged     #(Long/parseLong %)]
     [:bytes.read       #(sectors-to-bytes (Long/parseLong %))]
     [:time.read        #(Long/parseLong %)]
     [:writes           #(Long/parseLong %)]
     [:writes-merged    #(Long/parseLong %)]
     [:bytes.written    #(sectors-to-bytes (Long/parseLong %))]
     [:time.write       #(Long/parseLong %)]
     [:current-io       #(Long/parseLong %)]
     [:time.io          #(Long/parseLong %)]
     [:time.io-weighted #(Long/parseLong %)]]
    :row-filter
    #(not (re-find #"(loop|ram)" (get % :name))))
   :delete-key true
   :transform-row #(annotate-values % disk-annotations)))

(defn- disks []
  (case-os
   "Linux" (disks-linux)))

(observe :disk :ttl 5 :tags ["system" "disk"] :prefix "disk"
         (doseq [[disk-name disk] (seq (disks))]
           (println (disks))
           (submit-values disk :suffix (format "[%s]" disk-name))))

