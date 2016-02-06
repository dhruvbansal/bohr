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
  
(defn- populate! [runtime-options]
  (read-inputs! (get runtime-options :input))
  (if (not (observers?)) (log/warn "No observations defined!"))
  (check-undefined-dependencies!))

(def pool (mk-pool))
  
(defn- start! [runtime-options]
  (log/debug "Taking initial readings...")
  (for-each-observer (fn [name _] (take-reading! name (observe name)))))

(defn- periodically-observe! []
  (map-periodic-observers
   (fn [[name observer]]
     (let [ttl-in-ms (* 1000 (get observer :ttl))]
       (every
        ttl-in-ms
        #(take-reading! name (observe name))
        pool
        :initial-delay ttl-in-ms)))))

(defn- loop! [runtime-options]
  (log/debug "Setting up observer schedule!")
  (let [schedules (periodically-observe!)]
    (try
      (log/info "Bohr is now running")
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
