;;;; Observers are data structures consisting of a name, some options,
;;;; and a function (the observer's `instructions`).
;;;;
;;;; The function (which takes no arguments) should call the `submit`
;;;; or `submit-many` functions, see journals.clj.

(ns bohr.observers
  (:require [clojure.tools.logging :as log]))

;; The set of Bohr observers.
;;
;; Keys should be observer names (strings) and values are the observer
;; functions themselves.
(def ^{:private true} observers (atom {}))

;; Counter for the number of observations sucessfully made (across all
;; observers).
(def observations (atom 0))

(def ^{:dynamic true} current-observer nil)
(def ^{:dynamic true} current-prefix nil)
(def ^{:dynamic true} current-suffix nil)
(def ^{:dynamic true} current-units nil)
(def ^{:dynamic true} current-tags nil)

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
    - :period : The amount of time (in seconds) the observer's reading is considered current
    - :prefix : A prefix to add to the observer's name.
    - :suffix : A suffix to add to the observer's name.
    - :units : Units for the observer's reading.
    - :tags : Tags for the observer's reading.
  - `instructions` : A function (taking no arguments) which returns the observer's reading."
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
              current-prefix   (get observer :prefix)
              current-suffix   (get observer :suffix)
              current-units    (get observer :units)
              current-tags     (get observer :tags [])]
      ((get observer :instructions)))))

(defn- observer-allowed?
  "Is the observer with the given `observer-name` allowed to observe,
  given the `excluded-patterns` and `included-patterns`?"
  [observer-name excluded-patterns included-patterns]
  (cond
    (and
     (empty? excluded-patterns)
     (empty? included-patterns))
    true

    (and
     (not-empty excluded-patterns)
     (empty? included-patterns))
    (not-any?
     #(re-find % (name observer-name))
     excluded-patterns)

    (and
     (empty? excluded-patterns)
     (not-empty included-patterns))
    (some
     #(re-find % (name observer-name))
     included-patterns)

    :else
    (and
     (some
      #(re-find % (name observer-name))
      included-patterns)
     (not-any?
      #(re-find % (name observer-name))
      excluded-patterns))))

(defn- allowed-observers
  "Returns a sequence of observers that are allowed given the
  inclusion/exclusion patterns in `runtime-options`."
  [runtime-options]
  (let [excluded-patterns (map #(re-pattern %) (get runtime-options :exclude-observer))
        included-patterns (map #(re-pattern %) (get runtime-options :include-observer))]
    (filter
     (fn [[name observer]]
       (observer-allowed? name excluded-patterns included-patterns))
     (seq @observers))))
  
(defn- allowed-periodic-observers
  "Returns a sequence of observers that are allowed given the
  inclusion/exclusion patterns in `runtime-options` and that have periods
  defined."
  [runtime-options]
  (filter
   (fn [[name observer]]
     (get observer :period))
   (allowed-observers runtime-options)))

(defn map-observers
  "Returns the result of applying a function `f` over each observer.

  The function should take two arguments: the name of the observer and
  the observer itself.

  Only observers that are allowed given the inclusion/exclusion
  patterns in `runtime-options` will be iterated over.

  If the argument `periodic` evaluates to true, only observers with
  periods defined will be iterated over."
  [runtime-options periodic f]
  (map
   f
   (if periodic
     (allowed-periodic-observers runtime-options)
     (allowed-observers runtime-options))))

(defn for-each-observer
  "Successively apply a function `f` for each observer.

  The function should take two arguments: the name of the observer and
  the observer itself.

  Only observers that are allowed given the inclusion/exclusion
  patterns in `runtime-options` will be iterated over.

  If the argument `periodic` evaluates to true, only observers with
  periods defined will be iterated over."
  [runtime-options periodic f]
  (doseq [[name observer] 
          (if periodic
            (allowed-periodic-observers runtime-options)
            (allowed-observers runtime-options))]
    (f name observer)))

(defn warn-if-no-observers!
  "Logs a warning message if no observers are defined."
  []
  (if (not (observers?))
    (log/warn "No observers defined, Bohr has nothing to do!!")))
