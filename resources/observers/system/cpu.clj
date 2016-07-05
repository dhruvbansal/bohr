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

(defn- util-mac []
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

(def time-annotations
  {
    :user         { :desc "Time spent in user mode" }
    :nice         { :desc "Time spent in user mode with low priority (nice)" }
    :system       { :desc "Time spent in system mode" }
    :idle         { :desc "Time spent in the idle task." }
    :iowait       { :desc "Time waiting for I/O to complete" }
    :irq          { :desc "Time servicing interrupts" }
    :softirq      { :desc "Time servicing softirqs" }
    :steal        { :desc "Time stolen (spent in other OS when running in a VM)" }
    :guest        { :desc "Time spent running a VM" }
    :guest-nice   { :desc "Time spent running a niced guest" }
    })

(def util-annotations
  {
    :user         { :desc "% of time spent in user mode" }
    :system       { :desc "% of time spent in system mode" }
    :idle         { :desc "% of time spent in the idle task." }
    })

(defn- cpu-time []
  (annotate
   (case-os
    "Linux" (time-linux))
   time-annotations))

(defn- cpu-util []
  (annotate
   (case-os
    "Mac" (util-mac))
   util-annotations))
  
(observe :cpu.load :ttl 5, :tags ["system", "cpu"] :prefix "cpu.load"
         (submit-many (load-average)))

(observe :cpu.util :ttl 5, :tags ["system", "cpu"] :prefix "cpu.util" :units "%"
         (submit-many (cpu-util)))

(observe :cpu.time :ttl 5, :tags ["system", "cpu", "counter"] :prefix "cpu.time" :units "s"
         (submit-many (cpu-time)))
