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

(observe :users :period 5
         (do
           (submit "users.count" (user-count) :tags ["metric"] :desc "Total number of system users")
           (submit "groups.count" (group-count) :tags ["metric"] :desc "Total number of system groups")))
