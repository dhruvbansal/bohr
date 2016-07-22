;;; schedules.clj
;;;
;;; Defines methods for use by DSL code that needs to schedule some
;;; future event.
;;;
;;; Registering such needs lets Bohr's core wait for these events to
;;; complete before exiting (when necessary).

(ns bohr.dsl.schedules
  (:require [clojure.tools.logging :as log]))

(def ^{:private true} schedules (atom {}))

(defn scheduled?
  "Are there any functions scheduled by the DSL?"
  []
  (not (empty? @schedules)))

(defn done?
  "Are all DSL functions finished executing?"
  []
  (every? #(%) (vals @schedules)))

(defn schedule!
  "Register a scheduled function by the given name.

   The `done-function` should be a function (accepting 0 arguments)
  that returns true if the scheduled function is done with its work
  and false otherwise."
  [name done-function]
  (swap! schedules assoc name done-function))
