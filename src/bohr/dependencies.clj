(ns bohr.dependencies
  (:require [clojure.tools.logging :as log]))

(def ^{:private true} upstream   (atom {}))
(def downstream (atom {}))

(defn- prevent-circular-dependencies! [dependencies dependency-chain]
  (doseq [dependency dependencies]
    (let [extended-dependency-chain (conj dependency-chain dependency)]
      (if (= dependency (first dependency-chain))
        (throw (ex-info
                (format "Circular dependency among observers: %s" extended-dependency-chain)
                {:bohr true :type :bohr-circular-dependency-exception
                 :dependency-chain extended-dependency-chain})))
      (prevent-circular-dependencies!
       (get @upstream dependency (set []))
       extended-dependency-chain)))
  true)

(defn register-dependency! [dependent dependency]
  (let [existing-dependencies (get @upstream dependent (set []))]
    (if (not (contains? existing-dependencies dependency))
      (let [extended-dependencies (conj existing-dependencies dependency)]
        (do
          (prevent-circular-dependencies! extended-dependencies (vector dependent))
          (log/trace "Adding dependency of" dependent "on" dependency)
          (swap! upstream assoc dependent extended-dependencies)
          (let [existing-dependents (get @downstream dependency (set []))]
            (swap! downstream assoc dependency (conj existing-dependents dependent))))))))

(defn check-undefined-dependencies! [known-observer-names]
  (doseq [[observer dependencies] (seq @upstream)]
    (doseq [dependency dependencies]
      (if (not (contains? known-observer-names dependency))
        (throw
         (ex-info
          (format "Undefined dependency on observer '%s' in observer '%s'" dependency observer)
          {:bohr true :type :bohr-undefined-dependency}))))))

(defn downstream-of [name]
  (get @downstream name []))
