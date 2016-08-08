(defn- process-table []
  (parse-table
   (sh-output "ps -eo pid,user,state,etime,args -ww")
   [[:pid    :integer]
    [:user   :string]
    [:state  :string]
    [:etime  :string]
    [:cmd    :string]]
   :start-at 2
   :column-count 5))

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

(defn- submit-expected-process-count [table expected-process]
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
     :attrs { :name (:name expected-process) :agg "mean"})
    (if (= 1 matching-process-count)
      (assoc
      (first matching-processes)
      :name
      (:name expected-process)))))

(defn- process-procfile-contents [process path]
  (procfile-contents (format "%d/%s" (:pid process) path)))

(def ^{:private true} io-annotations
  {
   :reads                       { :desc "Number of read system calls"  }
   :writes                      { :desc "Number of write system calls" }
   :data.cache.read             { :desc "Data read, possibly from cache"      :units "B" }
   :data.cache.written          { :desc "Data written, possibly to cache"     :units "B" }
   :data.disk.read              { :desc "Data read from disk"                 :units "B" }
   :data.disk.written           { :desc "Data written to disk"                :units "B" }
   :data.disk.written.cancelled { :desc "Data written to disk then cancelled" :units "B" }
   })

(defn- submit-io [process]
  (submit-many
   (annotate
    (parse-properties
     (process-procfile-contents process "io")
     :converter #(Long/parseLong %)
     :translation
     {
      "syscr"                 :reads
      "syscw"                 :writes
      "rchar"                 :data.cache.read
      "wchar"                 :data.cache.written
      "read_bytes"            :data.disk.read
      "write_bytes"           :data.disk.written
      "cancelled_write_bytes" :data.disk.written.cancelled
      })
    io-annotations)
   :attrs { :name (:name process) :agg "last" :counter true }))

(def ^{:private true} stat-annotations
  {
   :state                 { :desc "Process state" :attrs { :agg "last"} }
   :time.user             { :desc "Time spent in user mode"   :units "s" :attrs { :agg "last" :counter true } }
   :time.system           { :desc "Time spent in system mode" :units "s" :attrs { :agg "last" :counter true } }
   :time.guest            { :desc "Time spent in hosting a guest VM" :units "s" :attrs { :agg "last" :counter true } }
   :time.io_delay         { :desc "Time spent waiting on block IO" :units "s" :attrs { :agg "last" :counter true } }
   :time.children.user    { :desc "Time spent by child processes in user mode" :units "s" :attrs { :agg "last" :counter true } }
   :time.children.system  { :desc "Time spent by child processes in system mode" :units "s" :attrs { :agg "last" :counter true } }
   :time.children.guest   { :desc "Time spent by child processes in hosting a guest VM" :units "s" :attrs { :agg "last" :counter true } }
   :faults.major          { :desc "Faults requiring loading a page from disk" :attrs { :agg "last" :counter true } }
   :faults.minor          { :desc "Faults not requiring loading a page from disk" :attrs { :agg "last" :counter true } }
   :faults.children.major { :desc "Faults by child processes requiring loading a page from disk" :attrs { :agg "last" :counter true } }
   :faults.children.minor { :desc "Faults by child processes not requiring loading a page from disk" :attrs { :agg "last" :counter true } }
   :priority              { :desc "Priority value"    :attrs { :agg "mean" } }
   :priority.rt           { :desc "Real-time priority value"    :attrs { :agg "mean" } }
   :nice                  { :desc "Nice value"        :attrs { :agg "mean" } }
   :threads               { :desc "Number of threads" :attrs { :agg "mean" } }
   :mem.virtual           { :desc "Virtual memory"    :units "B" :attrs { :agg "mean" }}
   :mem.resident          { :desc "Resident memory"   :units "B" :attrs { :agg "mean" }}
   })

(defn- submit-stats [process]
  (submit-many
   (annotate
    ;; You think you'll get it from `man proc`, but no...see
    ;; http://stackoverflow.com/questions/21773756/proc-pid-stat-file-unrecognizable-output
    (first
     (parse-table
      (process-procfile-contents process "stat")
      [[nil    identity] ; pid
       [nil    identity] ; comm
       [:state identity] ; state
       [nil    identity] ; ppid
       [nil    identity] ; pgrp
       [nil    identity] ; session
       [nil    identity] ; tty_nr
       [nil    identity] ; tpgid
       [nil    identity] ; flags
       [:faults.minor          :long] ; minflt
       [:faults.children.minor :long] ; cminflt
       [:faults.major          :long] ; majflt
       [:faults.children.major :long] ; cmajflt
       [:time.user            #(cpu-ticks-to-time (Long/parseLong %) 1)] ; utime
       [:time.system          #(cpu-ticks-to-time (Long/parseLong %) 1)] ; stime
       [:time.children.user   #(cpu-ticks-to-time (Long/parseLong %) 1)] ; cutime
       [:time.children.system #(cpu-ticks-to-time (Long/parseLong %) 1)] ; cstime
       [:priority :integer] ; priority
       [:nice     :integer] ; nice
       [:threads  :long] ; num_threads
       [nil identity] ; itrealvalue
       [nil identity] ; starttime
       [:mem.virtual  :long] ; vsize
       [:mem.resident #(* page-size (Long/parseLong %))] ; rss
       [nil identity] ; rsslim
       [nil identity] ; startcode
       [nil identity] ; endcode
       [nil identity] ; startstack
       [nil identity] ; kstkesp
       [nil identity] ; kstkeip
       [nil identity] ; signal
       [nil identity] ; blocked
       [nil identity] ; sigignore
       [nil identity] ; sigcatch
       [nil identity] ; wchan
       [nil identity] ; nswap
       [nil identity] ; cnswap
       [nil identity] ; exit_signal
       [nil identity] ; processor
       [:priority.rt :integer] ; rt_priority
       [nil identity] ; policy
       [:time.io_delay       #(cpu-ticks-to-time (Long/parseLong %) 1)] ; delayacct_blkio_ticks
       [:time.guest          #(cpu-ticks-to-time (Long/parseLong %) 1)] ; guest_time
       [:time.children.guest #(cpu-ticks-to-time (Long/parseLong %) 1)] ; cguest_time
       [nil identity] ; start_data
       [nil identity] ; end_data
       [nil identity] ; start_brk
       [nil identity] ; arg_start
       [nil identity] ; arg_end
       [nil identity] ; env_start
       [nil identity] ; env_end
       [nil identity] ; exit_code
       ]))
    stat-annotations)
   :attrs { :name (:name process) }))

(def ^{:private true} memory-stat-annotations
  {
   :mem.share { :desc "Shared memory (backed by file)" }
   :mem.text  { :desc "Memory used for text (code)"    }
   :mem.data  { :desc "Memory used for data and stack"  }
   })

(defn- submit-memory-stats [process]
  (submit-many
   (annotate
    (first
     (parse-table
      (process-procfile-contents process "statm")
      [[nil identity] ; size
       [nil identity] ; resident
       [nil identity] ; share
       [:mem.share #(* (Long/parseLong %) page-size)] ; text
       [:mem.text  #(* (Long/parseLong %) page-size)] ; lib
       [:mem.data  #(* (Long/parseLong %) page-size)] ; data
       [nil identity] ; dt
       ]))
    memory-stat-annotations)
   :units "B"
   :attrs { :name (:name process) :agg "mean" }))

(defn- elapsed-time [process]
  (let [match (re-find #"(?:(\d+)-)?(?:(\d{2}):)?(\d{2}):(\d{2})" (:etime process ""))]
    (if match
      (+
       (* (Integer/parseInt (or (match 1) "0")) 86400)
       (* (Integer/parseInt (or (match 2) "0")) 3600)
       (* (Integer/parseInt (or (match 3) "0")) 60)
       (Integer/parseInt (or (match 4) "0")))
      (do
        (log/error (format "Could not parse elapsed time of process '%s': %s"
                           (:name process)
                           (:etime process)))
        nil))))

(defn- submit-elapsed-time [process]
  (let [time (elapsed-time process)]
    (if time
      (submit "uptime"
              time
              :desc "Uptime"
              :units "s"
              :attrs { :agg "last" :name (:name process)}))))

(defn- submit-file-count [process]
  (submit
   "files"
   (Integer/parseInt
    (sh-output
     (format "ls /proc/%s/fd | wc -l" (:pid process))))
   :desc "Number of open files"
   :attrs { :name (:name process) :agg "mean" }))

(defn- submit-expected-process-observations [process]
  (submit-elapsed-time process)
  (submit-io process)
  (submit-stats process)
  (submit-memory-stats process)
  (submit-file-count process))

(defn- expected-processes []
  (or (get-config :processes.tracked) []))

(defn- observe-expected-processes? []
  (get-config :processes.tracked.observe))

(observe :ps :period 10 :prefix "ps"
         (let [table (process-table)]
           (submit-many (process-counts-by-state table) :attrs { :agg "mean" })
           (doseq [expected-process (expected-processes)]
             (let [matching-process (submit-expected-process-count table expected-process)]
               (if (and
                    (observe-expected-processes?)
                    matching-process)
                 (case-os
                  "Linux" (submit-expected-process-observations matching-process)))))))
