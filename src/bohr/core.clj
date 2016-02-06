(ns bohr.core
  (:require [clojure.tools.logging :as log])
  (:use
   overtone.at-at
   bohr.observers
   bohr.notebook
   bohr.journals
   bohr.inputs
   bohr.cli
   )
  (:gen-class))

(defn- force-get-reading! [name]
  (if (known-reading? name) (get-reading name)
      (do
        (take-reading! name (observe name))
        (get-reading name))))

(defn- refresh-reading! [name]
  (take-reading! name (observe name))
  (doseq [dependent (get @observer-has-dependents name [])]
    (refresh-reading! dependent)))
  
(defn- populate! [runtime-options]
  (read-inputs! (get runtime-options :input))
  (if (not (observers?)) (log/warn "No observations defined!"))
  (check-undefined-dependencies!))

(defn submit-in-observer!
  "Submit the metric from within the context of the given observer."
  [observer-name name value options]
  (submit-with-observer! (get-observer observer-name) name value options)
  nil)
  
(defn- start! [runtime-options]
  (log/debug "Taking initial readings...")
  (for-each-observer (fn [name _] (take-reading! name (observe name)))))

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
  (log/debug "Entering main loop!")
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

(defn- boot! [runtime-options]
  (try
    (log/debug "Bohr booting...")
    (populate! runtime-options)
    (start! runtime-options)
    (if (get runtime-options :daemon)
      (loop! runtime-options)
      (shutdown!))
    
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (log/error (.getMessage e))
        (throw e)))))

(defn -main [& cli-args]
  (boot! (parse-cli cli-args)))
