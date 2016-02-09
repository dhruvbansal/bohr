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

(defn- util-linux []
  (let [util-info
        (string/split (first (string/split-lines (procfile-contents "stat"))) #" +")
        
        util-names
        [:user :nice :system :idle :iowait :irq :softirq :steal :guest :guest-nice]
        
        util-values
        (map-indexed
         (fn [index name] (Integer/parseInt (nth util-info (+ index 1))))
         util-names)
        
        total-util
        (apply + util-values)

        normalized-util-values
        (map #(* 100.0 (float (/ % total-util))) util-values)
        ]
    (zipmap util-names normalized-util-values)))

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

(defn- util []
  (annotate-values
   (case-os
    "Linux" (util-linux)
    "Mac"   (util-mac))
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
    }))
  
(observe :cpu.count
         (cpu-count))

(observe :cpu.load :ttl 5, :tags ["system", "cpu"] :prefix "cpu.load"
         (submit-values (load-average)))

(observe :cpu.util :ttl 5, :tags ["system", "cpu"] :prefix "cpu.util" :units "%"
         (submit-values (util)))
