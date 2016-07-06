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
   bohr.dsl   
   bohr.scripts
   bohr.cli
   bohr.config   
   bohr.summary
   bohr.log
   )
  (:gen-class))

(defn- init!
  "Take initial readings from each observer."
  []
  (log/info "Taking initial readings...")
  (for-each-allowed-observer
   (fn [name _] (make-observation! name))))

(defn- stop!
  "Stop all scheduled observers."
  []
  (log/debug "Stopping all scheduled observers...")
  (doseq [[name schedule] (seq @observer-schedules)]
    (stop schedule)))
  
(def pool (mk-pool))

(defn- create-observer-schedule!
  "Start and return a new schedule for the given `observer` with the
  given `name`."
  [name observer]
  (log/debug (format "Scheduling observer %s to run every %ss" name (:period observer)))
  (let [period-in-ms (* 1000 (:period observer))]
    (every
     period-in-ms
     #(make-observation! name)
     pool
     :initial-delay period-in-ms)))

(defn- loop!
  "Run forever, with each observer taking readings on schedule given
  by its period."
  []
  (log/info "Periodically observing...")
  (try
    (for-each-periodic-allowed-observer
     (fn [name observer]
       (swap! observer-schedules assoc name
              (create-observer-schedule! name observer))))
    (catch clojure.lang.ExceptionInfo e
      (stop!)
      (throw e))))

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
    (load-config! runtime-options)
    (log/debug "Bohr is booting")
    (populate! input-paths)
    (warn-if-no-observers!)
    (if (or (get-config :loop) (get-config :submit))
      (ensure-some-journal!)
      (prepare-for-summarize!))
    (init!)
    (if (get-config :loop) (loop!))
    (if (and
         (not (get-config :submit))
         (not (get-config :loop)))
      (summarize!))
    (if (not (get-config :loop)) (exit!))
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (log/error (.getMessage e))
        (throw e)))))

(defn -main
  "Entry point for the `bohr` program."
  [& cli-args]
  (apply boot! (parse-cli cli-args)))
