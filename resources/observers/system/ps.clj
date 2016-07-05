(defn- process-table []
  (parse-table
   (sh-output "ps -eo pid,user,%cpu,rss,state,args -ww")
   [[:pid    :integer]
    [:user   :string]
    [:cpu    :float]
    [:memory 1024]
    [:state  :string]
    [:cmd    :string]]
   :start-at 2
   :column-count 6))

(defn- process-counts-by-state [table]
  (let [running  (atom 0)
        sleeping (atom 0)
        waiting  (atom 0)
        stopped  (atom 0)
        zombie   (atom 0)
        other    (atom 0)]
    (doseq [process table]
      (case (get process :state)
        ("S" "I") (swap! sleeping inc)
        "R"       (swap! running inc)
        ("D" "U") (swap! waiting inc)
        "T"       (swap! stopped inc)
        "Z"       (swap! zombie inc)
        (swap! other inc)))
    { :running { :value @running  :desc "Number of running processes"  }
     :sleeping { :value @sleeping :desc "Number of sleeping processes" }
     :waiting  { :value @waiting  :desc "Number of waiting processes"  }
     :stopped  { :value @stopped  :desc "Number of stopped processes"  }
     :zombie   { :value @zombie   :desc "Number of zombie processes"   }
     :other    { :value @other    :desc "Number of processes in other states" }}))

(def missing-process-state "X")

(defn- submit-expected-process-state [table expected-process-name expected-process-info]
  (let [metric-name 
        (format "state[%s]" (name expected-process-name))

        metric-desc
        (format "State of process %s" (name expected-process-name))

        user-pattern
        (re-pattern
         (if (map? expected-process-info)
           (or (get expected-process-info :user) #"\.")
           #"\."))
        
        cmd-pattern
        (re-pattern
         (if (map? expected-process-info)
           (or (get expected-process-info :cmd) #"\.")
           expected-process-info))
        
        process
        (first
         (filter
          #(and
            (re-find cmd-pattern  (get % :cmd))
            (re-find user-pattern (get % :user)))
          table))
        ]
    (submit
     metric-name
     (if process
       (get process :state)
       missing-process-state)
     :desc metric-desc
     :tags ["last"])))
        
(defn- expected-processes []
  (or (get-config :ps.expected) {}))

(observe :ps :period 5 :prefix "ps"
         (let [table (process-table)]
           (submit-many (process-counts-by-state table) :tags ["metric"])
           (doseq [[process-name process-info] (seq (expected-processes))]
             (submit-expected-process-state table process-name process-info))))

