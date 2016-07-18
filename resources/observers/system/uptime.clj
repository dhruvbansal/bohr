(def boot-time-formatter-linux (time-format/formatter "YYYY-MM-dd HH:mm:ss"))
(def boot-time-formatter-mac   (time-format/formatter "EEE MMM d HH:mm:ss YYYY"))

(defn- boot-time-linux []
  (time-format/parse
   boot-time-formatter-linux
   (sh-output "uptime -s")))

(defn- boot-time-mac []
  (time-format/parse
   boot-time-formatter-mac
   (string/replace
    (last (re-find #"\} (.*+)$" (sysctl "kern.boottime")))
    #" +"
    " " )))

(def boot-time
  (case-os
   "Linux" (boot-time-linux)
   "Mac"   (boot-time-mac)))

(defn- uptime-linux []
  (Float/parseFloat
   (first
    (string/split
     (procfile-contents "uptime")
     #" +"))))

(defn- uptime-mac []
  (time/in-seconds
   (time/interval boot-time (time/now))))

(defn- uptime []
  (case-os
   "Linux" (uptime-linux)
   "Mac"   (uptime-mac)))

(observe :uptime :period 60 :tags ["duration"] :units "s"
         (submit "uptime" (uptime) :desc "System uptime"))
