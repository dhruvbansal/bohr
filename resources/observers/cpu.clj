(def cpu-clock-tick
  (Float/parseFloat
   (sh-output "getconf CLK_TCK")))

(defn- cpu-count-linux []
  (count
   (filter
    #(re-find #"^processor" %)
    (string/split-lines (procfile-contents "cpuinfo")))))

(defn- cpu-count-mac []
  (Integer/parseInt (sysctl "hw.ncpu")))

(defn- cpu-count []
  (case-os
   "Linux" (cpu-count-linux)
   "Mac"   (cpu-count-mac)))

(defn- raw-load-average-linux []
  (take
    3
    (string/split
     (procfile-contents "loadavg")
     #" +")))

(defn- raw-load-average-mac []
  (rest
   (take
    4
    (string/split
     (sysctl "vm.loadavg")
     #" +"))))
  
(defn- raw-load-average []
  (case-os
   "Linux" (raw-load-average-linux)
   "Mac"   (raw-load-average-mac)))
   
(defn- load-average []
  (let [load-average-info
        (map #(Float/parseFloat %) (raw-load-average))]
    {:1  { :value (nth load-average-info 0) :desc "Average # of running processes (last 1 min.)"}
     :5  { :value (nth load-average-info 1) :desc "Average # of running processes (last 5 min.)"}
     :15 { :value (nth load-average-info 2) :desc "Average # of running processes (last 15 min.)"}}))

(defn- time-linux []
  (let [strings
        (string/split (first (string/split-lines (procfile-contents "stat"))) #" +")
        
        names
        [:user :nice :system :idle :iowait :irq :softirq :steal :guest :guest-nice]

        cpus
        (cpu-count)

        times
        (map-indexed
         (fn [index name]
           (/ 
            (Long/parseLong (nth strings (+ index 1)))
            (* cpus cpu-clock-tick)))
         names)]
    (zipmap names times)))

(def top-cpu-usage-regexp-mac #"CPU usage: +([\.\d]+)% +user, +([\.\d]+)% +sys, +([\.\d]+)% +idle")

(defn- time-mac []
  (let [percentages
        #(re-find
          top-cpu-usage-regexp-mac
          (sh-output "top -l 1 | head -n 5 | grep 'CPU usage'"))]
    (into
     {}
     (map-indexed
      (fn [index name]
        [name
         (Float/parseFloat
          (nth
           percentages
           (+ 1 index)))])
      [:user :system :idle]))))

(def time-annotations-linux
  {
    :user         { :desc "Time spent in user mode" :units "s" :tags ["counter"] }
    :nice         { :desc "Time spent in user mode with low priority (nice)" :units "s" :tags ["counter"] }
    :system       { :desc "Time spent in system mode" :units "s" :tags ["counter"] }
    :idle         { :desc "Time spent in the idle task" :units "s" :tags ["counter"] }
    :iowait       { :desc "Time waiting for I/O to complete" :units "s" :tags ["counter"] }
    :irq          { :desc "Time servicing interrupts" :units "s" :tags ["counter"] }
    :softirq      { :desc "Time servicing softirqs" :units "s" :tags ["counter"] }
    :steal        { :desc "Time stolen (spent in other OS when running in a VM)" :units "s" :tags ["counter"] }
    :guest        { :desc "Time spent running a VM" :units "s" :tags ["counter"] }
    :guest-nice   { :desc "Time spent running a niced guest" :units "s" :tags ["counter"] }
    })

(def time-annotations-mac
  {
   :user         { :desc "Time spent in user mode"     :units "%" :tags ["metric"] }
   :system       { :desc "Time spent in system mode"   :units "%" :tags ["metric"] }
   :idle         { :desc "Time spent in the idle task" :units "%" :tags ["metric"] }
   })

(defn- cpu-time []
  (case-os
   "Linux"
   (annotate (time-linux) time-annotations-linux)
   "Mac"
   (annotate (time-mac) time-annotations-mac)))

(observe :cpu.load :period 5, :prefix "cpu.load" :tags ["metric"]
         (submit-many (load-average)))

(observe :cpu.time :period 5, :prefix "cpu.time"
         (submit-many (cpu-time)))
