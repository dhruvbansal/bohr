;;;; The results of calling observer functions are stored in a
;;;; 'notebook' which is a map of observer names to names of Clojure
;;;; atoms which store the latest reading for that observer.
;;;;
;;;; A single atom could have been used to store all readings from all
;;;; observers and then updated when any observer made a new reading.
;;;; I was worried about the efficiency of this and so elected to use
;;;; a collection of one-atom-per-observer.  Since different observers
;;;; are on different TTLs, this should minimize both the number of
;;;; updates and the "throughput" required for each.
;;;;
;;;; Premature optimization or smart engineering?  I don't know...

(ns bohr.notebook
  (:require [clojure.tools.logging :as log]))

;; Map of observer names to Clojure atom names.
(def ^{:private true} atomic-symbols (atom {}))

(defn- atomic-symbol-for
  "Return the name of the Clojure atom containing the latest reading
  for the observer with the given `name`."
  [name]
  (get @atomic-symbols name))

(defn- atom-for
  "Return the Clojure atom containing the latest reading for the
  observer with the given `name`."
  [name]
  (deref (intern 'bohr.core (atomic-symbol-for name))))

(defn- define-new-atom!
  "Define a new atom with the initial `state` to hold the latest
  reading for the observer with the given `name`."
  [name state]
  (log/trace (format "Creating value for %s --> %s" name state))
  (swap! atomic-symbols assoc name (gensym "bohr-"))
  (intern 'bohr.core (atomic-symbol-for name) (atom state)))

(defn reading?
  "Is there an existing reading for the observer of the given `name`?"
  [name]
  (contains? @atomic-symbols name))

(defn get-reading
  "Return the last reading for the observer of the given `name`.

  Returns `nil` if no reading exists."
  [name]
  (if (reading? name)
    @(atom-for name)
    (do
      (log/warn "Attempt to get reading" name "before it was taken!")
      nil)))

(defn take-reading!
  "Store a new `state` as the last reading for the observer of the
  given `name`."
  [name state]
  (if (not (reading? name))
    (define-new-atom! name state)
    (do
      (log/trace (format "Updating value for %s --> %s" name state))
      (reset! (atom-for name) state))))
