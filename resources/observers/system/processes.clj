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

(def default-user-pattern-string  ".")
(def default-cmd-pattern-string   ".")
(def default-state-pattern-string "[^Z]")

(defn- submit-expected-process-state [table expected-process]
  (let [metric-desc
        (format "Number of %s processes" (:name expected-process))

        user-pattern
        (re-pattern
         (or
          (get expected-process :user)
          default-user-pattern-string))
        
        cmd-pattern
        (re-pattern
           (or
            (get expected-process :cmd)
            default-cmd-pattern-string))

        state-pattern
        (re-pattern
           (or
            (get expected-process :state)
            default-state-pattern-string))
        
        matching-processes
        (filter
         #(and
           (re-find cmd-pattern   (get % :cmd))
           (re-find user-pattern  (get % :user))
           (re-find state-pattern (get % :state)))
         table)
        
        matching-process-count
        (count matching-processes)
        ]
    (submit
     "count"
     matching-process-count
     :desc metric-desc
     :attributes { :name (:name expected-process) }
     :tags ["metric"])))
        
(defn- expected-processes []
  (or (get-config :processes.tracked) []))

(observe :ps :period 10 :prefix "ps"
         (let [table (process-table)]
           (submit-many (process-counts-by-state table) :tags ["metric"])
           (doseq [expected-process (expected-processes)]
             (submit-expected-process-state table expected-process))))
