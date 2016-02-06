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
  (for-each-observer #(take-reading! (observe %))))

(defn- periodically-observe! []
  (map-periodic-observers
   (fn [name observer]
     (every
      (get observer :ttl)
      #(take-reading! (observe name))
      pool
      :initial-delay (get observer :ttl)))))

(defn- loop! []
  (log/info "Bohr is now running!")
  (let [schedules (periodically-observe!)]
    (try
      ;; ...?
      nil
      (catch clojure.lang.ExceptionInfo e
        (doseq [schedule schedules] (stop schedule))
        (throw e)))))

(defn- boot! [runtime-options]
  (try
    (log/debug "Bohr booting...")
    (populate! runtime-options)
    (start! runtime-options)
    (if (get runtime-options :daemon) (loop! runtime-options))
    
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (log/error (.getMessage e))
        (throw e)))))

(defn -main [& cli-args]
  (boot! (parse-cli cli-args)))
