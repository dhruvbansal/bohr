(ns bohr.core
  (:require [clojure.tools.logging :as log])
  (:use
   overtone.at-at
   bohr.observers
   bohr.notebook
   bohr.journals
   bohr.dsl   
   bohr.scripts
   bohr.cli
   )
  (:gen-class))

(defn- refresh-reading! [name]
  (take-reading! name (make-observation name))
  (doseq [dependent (get @observer-has-dependents name [])]
    (refresh-reading! dependent)))

(defn- populate! [input-paths]
  (read-inputs! input-paths)
  (check-undefined-dependencies!)
  (if (not (observers?)) (log/warn "No observers defined, Bohr has nothing to do!!")))

(defn- start! [runtime-options]
  (log/info "Taking initial readings")
  (for-each-observer (fn [name _] (take-reading! name (make-observation name)))))

(def pool (mk-pool))

(defn- periodically-observe! []
  (map-periodic-observers
   (fn [[name observer]]
     (let [ttl-in-ms (* 1000 (get observer :ttl))]
       (every
        ttl-in-ms
        #(refresh-reading! name)
        pool
        :initial-delay ttl-in-ms)))))

(defn- loop! [runtime-options]
  (log/info "Entering main loop!")
  (let [schedules (periodically-observe!)]
    (try
      ;; For some reason, this `doseq' block must be here in order for
      ;; the timers to run...perhaps because they need to be
      ;; explicitly mentioned somewhere or the compiler will just
      ;; optimize them out?
      (doseq [schedule schedules] schedule)
      (catch clojure.lang.ExceptionInfo e
        (doseq [schedule schedules] (stop schedule))
        (throw e)))))

(defn- shutdown! []
  (exit 0 "Bohr is exiting"))

(defn- boot! [input-paths runtime-options]
  (try
    (log/debug "Bohr is booting")
    (populate! input-paths)
    (start! runtime-options)
    (if (get runtime-options :loop)
      (loop! runtime-options)
      (shutdown!))
    
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (log/error (.getMessage e))
        (throw e)))))

(defn -main [& cli-args]
  (apply boot! (parse-cli cli-args)))
