;;;; The notebook stores the last readings of each observer by
;;;; providing a translation between the name of an observer (a
;;;; `java.lang.String`) and its last reading (the state of a Clojure
;;;; `atom`).

(ns bohr.notebook
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]))

(def ^{:private true} atomic-symbols (atom {}))

(defn- atomic-symbol-for [name]
  (get @atomic-symbols name))

(defn- define-new-atom! [name state]
  (log/trace "Defining new atom for" name "with initial state" state)
  (swap! atomic-symbols assoc name (gensym "bohr-"))
  (intern 'bohr.core (atomic-symbol-for name) (atom state)))

(defn- atom-for [name]
  (deref (intern 'bohr.core (atomic-symbol-for name))))

(defn known-reading? [name]
  (contains? @atomic-symbols name))

(defn get-reading
  "Return the last reading for the given observer, or `nil` if no "
  [name]
  (if (known-reading? name)
    @(atom-for name)
    (do
      (log/warn "Attempt to get reading" name "before it was taken!")
      nil)))

(defn take-reading!
  "Store the given reading by the given name."
  [name state]
  (if (not (known-reading? name))
    (define-new-atom! name state)
    (do
      (log/trace "Updating atom for" name "with state" state)
      (reset! (atom-for name) state))))
