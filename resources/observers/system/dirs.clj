(def dir-annotations
  {
   :size  { :desc "Total size of directory" :units "B" }
   :files { :desc "Total number of files" }
   })

(defn- dirs-to-track []
  (or (get-config :directories.tracked) {}))

(defn- dir-size [dir]
  (Long/parseLong
   (sh-output
    (format "du -s --block-size=1 %s 2>/dev/null | cut -f 1" (:path dir)))))

(defn- file-count [dir]
  (Long/parseLong
   (sh-output
    (format "find %s/ -type f -xdev 2>/dev/null | wc -l" (:path dir)))))

(defn- dir-summary [dir]
  (annotate
   {:size (dir-size dir) :files (file-count dir) }
   dir-annotations))
   
(observe :dir :period 300 :prefix "dir" :attrs { :agg "mean" }
         (doseq [dir (seq (dirs-to-track))]
           (submit-many (dir-summary dir) :attrs { :role (:role dir) :path (:path dir) })))
