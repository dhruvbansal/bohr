(defn- file-checksum-linux [file]
  (first
   (string/split
    (sh-output (format "md5sum %s" (:path file)))
    #" " 2)))

(defn- file-checksum-mac [file]
  (last
   (string/split
    (sh-output (format "md5 %s" (:path file)))
    #" ")))

(defn file-checksum [file]
  (case-os
   "Linux" (file-checksum-linux file)
   "Mac"   (file-checksum-mac file)))

(def file-stats-annotations
    {:lines { :desc "Number of lines" }
     :words { :desc "Number of words" }
     :size  { :desc "File size" :units "B"}})

(defn file-stats [file]
  (let [values
        (string/split
         (sh-output (format "wc %s" (:path file)))
         #" +")]
    (annotate
     {:lines (Long/parseLong (get values 0))
      :words (Long/parseLong (get values 1))
      :size  (Long/parseLong (get values 2))}
     file-stats-annotations)))

(defn- tracked-files []
  (or (get-config :files.tracked) []))

(observe :file :period 60 :prefix "file"
         (doseq [file (tracked-files)]
           (submit
            "checksum"
            (file-checksum file)
            :desc (format "MD5 checksum of %s" (:path file))
            :attrs { :role (:role file) :path (:path file) :agg "last"})
           (submit-many (file-stats file) :attrs { :role (:role file ) :path (:path file) :agg "mean"})))
