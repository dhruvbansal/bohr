;;;; This namespace ties together several parts of Bohr.
;;;;
;;;; It provides the the `-main` and `boot` functions which start the
;;;; `bohr` process.
;;;;
;;;; It triggers instantiating the logger, reading configuration, and
;;;; populating Bohr's observers & journals.
;;;;
;;;; It handles scheduling observers to refresh their readings in the
;;;; future and acts as the nexus between observers, the notebook, and
;;;; journals.

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
   bohr.log
   )
  (:gen-class))

(defn- refresh-reading!
  "Refresh the reading for the observer of the given `name` and all
  its dependents."
  [name]
  (take-reading! name (make-observation name))
  (doseq [dependent (downstream-of name)]
    (refresh-reading! dependent)))

(defn- populate!
  "Populate Bohr's observers and journals from the given `input-paths`
  and `runtime-options`.

  Will delegate to the functions:
  - `load-bundled-observers!`
  - `load-bundled-journals`
  - `load-scripts!`"
  [input-paths runtime-options]
  (load-bundled-observers! runtime-options)
  (load-bundled-journals! runtime-options)
  (load-scripts! input-paths)
  (warn-if-no-observers!)
  (check-undefined-dependencies! (observer-names)))
  
(defn- start!
  "Take initial readings from each observer."
  [runtime-options]
  (log/info "Taking initial readings...")
  (for-each-observer
   runtime-options
   false ; want to include observers without TTLs
   (fn [name _] (take-reading! name (make-observation name)))))

(def pool (mk-pool))

(defn- create-observer-schedules!
  "Schedules each observer with a TTL to refresh its reading periodically.

  The sequence of schedules is returned."
  [runtime-options]
  (map-observers
   runtime-options
   true ; only want observers with TTLs
   (fn [[name observer]]
     (let [ttl-in-ms (* 1000 (get observer :ttl))]
       (log/debug (format "Scheduling observer %s to run every %ss" name (:ttl observer)))
       (every
        ttl-in-ms
        #(refresh-reading! name)
        pool
        :initial-delay ttl-in-ms)))))

(defn- loop!
  "Run forever, with each observer taking readings on schedule given
  by its TTL."
  [runtime-options]
  (log/info "Periodically observing...")
  (let [schedules (create-observer-schedules! runtime-options)]
    (try
      ;; For some reason, this `doseq' block must be here in order for
      ;; the timers to run...perhaps because they need to be
      ;; explicitly mentioned somewhere or the compiler will just
      ;; optimize them out?
      (doseq [schedule schedules] schedule)
      (catch clojure.lang.ExceptionInfo e
        (doseq [schedule schedules] (stop schedule))
        (throw e)))))

(defn- boot!
  "Boot Bohr process.

  Takes the following steps:

  1) Initiates the logger.
  2) Loads configuration files.
  3) Populates observers & journals.
  4) Choose what happens:
     a) If --loop was given, run forever, submitting observations to
        journals.  If no journals were populated, defaults to the console
        journal.
     b) If --submit was given, run once, submitting initial
        observations to journals.  If no journals were populated, defaults
        to the console journal.
     c) Otherwise run once and summarize all observations to console,
        ignoring all journals."
  [input-paths runtime-options]
  (set-bohr-logger! runtime-options)
  (try
    (log/debug "Bohr is booting")
    (load-config! runtime-options)
    (populate! input-paths runtime-options)
    (if (or (get runtime-options :loop) (get runtime-options :submit))
      (ensure-some-journal!)
      (prepare-for-summarize! runtime-options))
    (start! runtime-options)
    (if (get runtime-options :loop) (loop! runtime-options))
    (if (and
         (not (get runtime-options :submit))
         (not (get runtime-options :loop)))
      (summarize! runtime-options))
    (if (not (get runtime-options :loop)) (exit!))
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (log/error (.getMessage e))
        (throw e)))))

(defn -main
  "Entry point for the `bohr` program."
  [& cli-args]
  (apply boot! (parse-cli cli-args)))
