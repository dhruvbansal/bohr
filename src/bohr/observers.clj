;;;; Observers are functions decorated with some options (see
;;;; below).  Each observer's function takes no arguments and returns
;;;; a value: that observer's reading.
;;;;
;;;; Observers' functions can refer to the current readings of other
;;;; observers, creating a dependency graph among observers.  Circular
;;;; dependencies are prevented by searching for them when new
;;;; observers are defined.
;;;;
;;;; Undefined dependencies should be checked for once all observers
;;;; are defined but have not started taking readings.

(ns bohr.observers)

(def ^{:private true} observers (atom {}))

(defn- known-observer? [name]
  (contains? @observers name))

(defn observer-count []
  (count @observers))

(defn observers? []
  (< 0 (observer-count)))

(defn get-observer
  "Return the observer of the given `name`.  If `option` is provided,
  return the observer's matching option instead."
  [name option]
  (if (known-observer? name)
    (let [observer (get @observers name)]
      (if option
        (get observer option)
        observer))
    (throw
     (ex-info
      (format "No such observer '%s'" name)
      {:bohr true :type :bohr-no-such-observer-exception}))))

(def observer-depends-on (atom {}))

(def ^{:private true} observer-has-dependents (atom {}))

(defn- prevent-circular-dependencies! [dependencies dependency-chain]
  (doseq [dependency dependencies]
    (let [extended-dependency-chain (conj dependency-chain dependency)]
      (if (== dependency (nth dependency-chain 0))
        (throw (ex-info
                (format "Circular dependency among observers: %s" extended-dependency-chain)
                {:bohr true :type :bohr-circular-dependency-exception
                 :dependency-chain extended-dependency-chain})))
      (prevent-circular-dependencies!
       (get @observer-depends-on dependency [])
       extended-dependency-chain))))

(defn define-observer!
  "Define a new observer."
  [name options instructions dependencies]
  (prevent-circular-dependencies! dependencies (vector name))
  (let [new-options (assoc options :instructions instructions)]
    (swap! observers assoc name new-options))
  (swap! observer-depends-on assoc name dependencies)
  (for [dependency dependencies]
    (let [current-dependents (get @observer-has-dependents dependency [])
          new-dependents     (conj current-dependents name)]
      (swap! observer-has-dependents assoc dependency current-dependents))))

(defn check-undefined-dependencies! []
  (doseq [[observer dependencies] (seq @observer-depends-on)]
    (doseq [dependency dependencies]
      (try
        (get-observer dependency)
        (catch clojure.lang.ExceptionInfo e
          (if (= :bohr-no-such-observer-exception (-> e ex-data :type))
            (throw
             (ex-info
              (format "Undefined dependency on observer '%s' in observer '%s'" dependency observer)
              {:bohr true :type :bohr-undefined-dependency}))))))))

(defn observe
  "Make an observation."
  [name]
  ;; FIXME add error handling here...
  (apply (get-observer name :instructions)))

(defn for-each-observer
  "Iterate over all observers."
  [f]
  (doseq [[name observer] @observers]
    (apply f name observer)))

(defn map-periodic-observers
  "Map over all periodic observers."
  [f]
  (map
   (filter @observers (fn [observer] (get observer :ttl)))
   f))
