;;;; Observers are data structures consisting of a name, some options,
;;;; and a function (the observer's `instructions`).
;;;;
;;;; The function (which takes no arguments) should call the `submit`
;;;; or `submit-many` functions, see journals.clj.

(ns bohr.observers
  (:require [clojure.tools.logging :as log])
  (:use bohr.config
        bohr.utils))

;; The set of Bohr observers.
;;
;; Keys should be observer names (strings) and values are the observer
;; functions themselves.
(def ^{:private true} observers (atom {}))

;; The set of schedules defined for (periodic) observers.
;;
;; Keys should be observer names (strings) and values are the
;; corresponding schedules.
(def observer-schedules (atom {}))

;; Counter for the number of observations sucessfully made (across all
;; observers).
(def observations (atom 0))

(def ^{:dynamic true} current-observer nil)
(def ^{:dynamic true} current-prefix nil)
(def ^{:dynamic true} current-suffix nil)
(def ^{:dynamic true} current-units nil)
(def ^{:dynamic true} current-tags nil)
(def ^{:dynamic true} current-attributes nil)

(defn- observer?
  "Is there an observer with the given `name`?"
  [name]
  (contains? @observers name))

(defn observer-count
  "The total number of observers."
  []
  (count @observers))

(defn observer-names
  "All observer names."
  []
  (keys @observers))

(defn observers?
  "Are there any observers?"
  []
  (< 0 (observer-count)))

(defn get-observer
  "Return the observer of the given `name`.  If `option` is provided,
  return the observer's matching option instead."
  ([name]
   (get-observer name nil))
  ([name option]
   (if (observer? name)
     (let [observer (get @observers name)]
       (if option
         (get observer option)
         observer))
     (throw
      (ex-info
       (format "No such observer '%s'" name)
       {:bohr true :type :bohr-no-such-observer-exception})))))

(defn define-observer!
  "Define a new observer.

  Parameters:

  - `name` : The name of the new observer
  - `options` : A map of options for the observer with the following keys:
    - :period : The amount of time (in seconds) the observer will wait between observations.
    - :prefix : A prefix to add to the name of any submissions made by the observer.
    - :suffix : A suffix to add to the name of any submissions made by the observer.
    - :units : Default units for any submissions made by the observer.
    - :tags : Default tags for any submissions made by the observer.
    - :attributes : Default key-value pairs for any submissions made by the observer.
  - `instructions` : A function (taking no arguments) which makes
  observations and calls the `submit` or `submit-many` functions in
  journals.clj."
  [name options instructions]
  (log/trace "Defining observer" name "with options" options)
  (let [new-options (assoc options :instructions instructions)]
    (swap! observers assoc name new-options)))

(defn make-observation!
  "Make observations using the observer of the given `name`.

  Increments the `observations` counter."
  [name]
  ;; FIXME add error handling here...
  (log/debug "Observing" name)
  (swap! observations inc)
  (let [observer (get-observer name)]
    (binding [current-observer name
              current-prefix     (get observer :prefix)
              current-suffix     (get observer :suffix)
              current-units      (get observer :units)
              current-tags       (get observer :tags [])
              current-attributes (get observer :attributes {})]
      ((get observer :instructions)))))

(defn- allowed-observers
  "Returns a sequence of observers that are allowed given the
  configured inclusion/exclusion patterns."
  []
  (filter
   (fn [[name observer]]
     (allowed? name (get-config :exclude-observers []) (get-config :include-observers [])))
   (seq @observers)))

(defn for-each-allowed-observer
  "Successively apply a function `f` for each observer.

  The function should take two arguments: the name of the observer and
  the observer itself.

  Only observers that are allowed given the configured
  inclusion/exclusion patterns will be iterated over."
  [f]
  (doseq [[name observer] (allowed-observers)]
    (f name observer)))

(defn for-each-periodic-allowed-observer
  "Successively apply a function `f` for each observer.

  The function should take two arguments: the name of the observer and
  the observer itself.

  Only observers that are allowed given the configured
  inclusion/exclusion patterns and which are defined with periods will
  be iterated over."
  [f]
  (for-each-allowed-observer
   (fn [name observer]
     (if (:period observer)
       (f name observer)))))

(defn warn-if-no-observers!
  "Logs a warning message if no observers are defined."
  []
  (if (not (observers?))
    (log/warn "No observers defined, Bohr has nothing to do!!")))
