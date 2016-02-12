(ns bohr.core
  (:require [clojure.tools.logging :as log])
  (:use
   overtone.at-at
   bohr.observers
   bohr.notebook
   bohr.journals
   bohr.dependencies
   bohr.dsl   
   bohr.scripts
   bohr.cli
   bohr.config   
   bohr.summary
   )
  (:gen-class))

(defn- refresh-reading! [name]
  (take-reading! name (make-observation name))
  (doseq [dependent (downstream-of name)]
    (refresh-reading! dependent)))

(defn- populate! [input-paths runtime-options]
  (read-bundled-observers! runtime-options)
  (read-inputs! input-paths)
  (check-undefined-dependencies! (observer-names))
  (read-bundled-journals! runtime-options))

(defn- start! [runtime-options]
  (log/info "Taking initial readings")
  (for-each-observer
   runtime-options
   (fn [name _] (take-reading! name (make-observation name)))))

(def pool (mk-pool))

(defn- periodically-observe! [runtime-options]
  (map-periodic-observers
   runtime-options
   (fn [[name observer]]
     (let [ttl-in-ms (* 1000 (get observer :ttl))]
       (every
        ttl-in-ms
        #(refresh-reading! name)
        pool
        :initial-delay ttl-in-ms)))))

(defn- loop! [runtime-options]
  (log/info "Entering main loop!")
  (let [schedules (periodically-observe! runtime-options)]
    (try
      ;; For some reason, this `doseq' block must be here in order for
      ;; the timers to run...perhaps because they need to be
      ;; explicitly mentioned somewhere or the compiler will just
      ;; optimize them out?
      (doseq [schedule schedules] schedule)
      (catch clojure.lang.ExceptionInfo e
        (doseq [schedule schedules] (stop schedule))
        (throw e)))))

(defn- print-version-and-exit! []
  (println (System/getProperty "bohr.version"))
  (exit 0))

(defn- shutdown! []
  (exit 0))

(defn- boot! [input-paths runtime-options]
  (try
    (log/debug "Bohr is booting")
    (if (get runtime-options :version) (print-version-and-exit!))
    (load-config! runtime-options)
    (populate! input-paths runtime-options)
    (check-for-observers!)
    (if (get runtime-options :loop)
      (do
        (check-for-journals!)
        (start! runtime-options)
        (loop! runtime-options))
      (do
        (prepare-for-summarize! runtime-options)
        (start! runtime-options)
        (summarize! runtime-options)
        (shutdown!)))
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (log/error (.getMessage e))
        (throw e)))))

(defn -main [& cli-args]
  (apply boot! (parse-cli cli-args)))
