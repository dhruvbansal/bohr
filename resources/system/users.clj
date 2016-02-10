(defn- user-count []
  (case-os
   ("Linux" "Mac")
   (with-open [rdr (clojure.java.io/reader "/etc/passwd")]
     (count (line-seq rdr)))))

(defn- group-count []
  (case-os
   ("Linux" "Mac")
   (with-open [rdr (clojure.java.io/reader "/etc/group")]
     (count (line-seq rdr)))))

(observe :users :ttl 5 :tags ["system"]
         (do
           (submit "users.count" (user-count) :tags ["users"] :desc "Total number of system users")
           (submit "groups.count" (group-count) :tags ["groups"] :desc "Total number of system groups")))
