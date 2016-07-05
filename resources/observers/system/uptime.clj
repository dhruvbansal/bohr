(def boot-time-formatter-linux (time-format/formatter "YYYY-MM-dd HH:mm:ss"))
(def boot-time-formatter-mac   (time-format/formatter "EEE MMM d HH:mm:ss YYYY"))

(defn- boot-time-linux []
  (time-format/parse
   boot-time-formatter-linux
   (sh-output "uptime -s")))

(defn- uptime-linux []
  (Float/parseFloat
   (first
    (string/split
     (procfile-contents "uptime")
     #" +"))))

(defn- boot-time-mac []
  (time-format/parse
   boot-time-formatter-mac
   (string/replace
    (last (re-find #"\} (.*+)$" (sysctl "kern.boottime")))
    #" +"
    " " )))

(defn- uptime-mac []
  (time/in-seconds
   (time/interval (& :boot-time) (time/now))))

(observe :boot-time
         (case-os
           "Linux" (boot-time-linux)
           "Mac"   (boot-time-mac)))

           
(observe :uptime :ttl 5 :tags ["duration"] :units "s"
         (submit "uptime" (case-os
                            "Linux" (uptime-linux)
                            "Mac"   (uptime-mac)) :desc "System uptime"))

