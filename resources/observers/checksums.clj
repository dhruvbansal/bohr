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

(defn- checksum-files []
  (or (get-config :checksum.files) []))

(observe :checksums :period 60 :prefix "checksum"
         (doseq [file (checksum-files)]
           (submit
            "file"
            (file-checksum file)
            :desc (format "MD5 checksum of %s" (:path file))
            :attributes { :role (:role file) }
            :tags ["last"])))
