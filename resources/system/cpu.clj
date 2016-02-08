(defn- count-linux []
  (count
   (filter
    #(re-find #"^processor" %)
    (string/split-lines (procfile-contents "cpuinfo")))))

(defn- count-mac []
  (Integer/parseInt (sysctl "hw.ncpu")))

(defn- load-average-linux []
  (let [load-average-info
        (map
         #(Float/parseFloat %)
         (take
          3
          (string/split
           (procfile-contents "loadavg")
           #" +")))]
    {:1  (nth load-average-info 0)
     :5  (nth load-average-info 1)
     :15 (nth load-average-info 2)}))

(defn- load-average-mac []
  (let [load-average-info
        (map
         #(Float/parseFloat %)
         (rest
          (take
           4
           (string/split
            (sysctl "vm.loadavg")
            #" +"))))]
    {:1  (nth load-average-info 0)
     :5  (nth load-average-info 1)
     :15 (nth load-average-info 2)}))

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
      [:user :sys :idle]))))

(observe :cpu.count
         (case-os
           "Linux" (count-linux)
           "Mac"   (count-mac)))

(observe :cpu.load :ttl 5, :tags ["system", "cpu"] :prefix "cpu.load"
         (submit-values
          (case-os
           "Linux" (load-average-linux)
           "Mac"   (load-average-mac))))

(observe :cpu.util :ttl 5, :tags ["system", "cpu"] :prefix "cpu.util" :units "%"
         (submit-values
          (case-os
           "Linux" (util-linux)
           "Mac"   (util-mac))))
