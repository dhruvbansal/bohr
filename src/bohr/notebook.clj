;;;; The notebook stores the last readings of each observer by
;;;; providing a translation between the name of an observer (a
;;;; `java.lang.String`) and its last reading (the state of a Clojure
;;;; `atom`).

(ns bohr.notebook
  (:require [clj-time.core :as time]))

(def ^{:private true} reading-numbers (atom {}))
(def ^{:private true} current-reading-number (atom 1))

(defn- known-reading? [name]
  (contains? @reading-numbers name))

(defn- reading-name-for [name]
  (format "bohr-reading-%i" (get @reading-numbers name)))

(defn- define-new-reading! [name]
  (swap! reading-numbers assoc name @current-reading-number)
  (swap! current-reading-number inc)
  (intern 'user (symbol (reading-name-for name)) (atom nil)))

(defn get-reading
  "Return the last reading for the given observer."
  [name]
  (if (known-reading? name)
    (deref (symbol (reading-name-for name)))))

(defn take-reading!
  "Store the given reading by the given name."
  [name value]
  (if (not (known-reading? name))
    (define-new-reading! name))
  (reset! (symbol (reading-name-for name)) value))
