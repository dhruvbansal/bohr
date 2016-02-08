(defn- boot-time-linux []
  ;; e.g. - 2016-01-31 01:58:41
  (time-format/parse
   (time-format/formatter "YYYY-MM-dd HH:mm:ss")
   (sh-output "uptime -s")))

(defn- uptime-linux []
  (Float/parseFloat
   (first
    (string/split
     (procfile-contents "uptime")
     #" +"))))

(defn- boot-time-mac []
  ;; e.g. - 2016-01-31 01:58:41
  (time-format/parse
   (time-format/formatter "EEE MMM d HH:mm:ss YYYY")
   (string/replace
    (last (re-find #"\} (.*+)$" (sysctl "kern.boottime")))
    #" +"
    " " )))

(defn- uptime-mac [boot-time]
  (time/in-seconds
   (time/interval boot-time (time/now))))

(observe :boot-time
         (case (:os.type)
           "Linux" (boot-time-linux)
           "Mac"   (boot-time-mac)
           (log/error "Cannot observe boot-time for OS" (:os.type))))
           
(observe :uptime :ttl 5 :tags ["system"] :units "s"
         (submit "uptime" (case (:os.type)
                            "Linux" (uptime-linux)
                            "Mac"   (uptime-mac (:boot-time))
                            (log/error "Cannot observe uptime for OS" (:os.type)))))
