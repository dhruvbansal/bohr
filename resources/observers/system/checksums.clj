(defn- file-checksum-linux [path]
  (first
   (string/split
    (sh-output (format "md5sum %s" path))
    #" " 2)))

(defn- file-checksum-mac [path]
  (last
   (string/split
    (sh-output (format "md5 %s" path))
    #" ")))

(defn file-checksum [path]
  (case-os
   "Linux" (file-checksum-linux path)
   "Mac"   (file-checksum-mac path)))

(defn- submit-file-checksum [path-info]
  (let [path
        (if (map? path-info) (get path-info :path) path-info)

        metric-name 
        (format "file[%s]" path)

        metric-desc
        (format "MD5 checksum of %s" path)]
    (submit
     metric-name
     (file-checksum path)
     :desc metric-desc
     :tags ["last"])))

(defn- checksum-files []
  (or (get-config :checksum.files) []))

(observe :checksums :period 5 :prefix "checksum"
         (doseq [path-info (checksum-files)]
           (submit-file-checksum path-info)))
