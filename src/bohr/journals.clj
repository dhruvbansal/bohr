(ns bohr.journals
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:use bohr.observers))

(def ^{:dynamic true} journals (atom {}))
(def submissions  (atom 0))
(def publications (atom 0))
(def disabled-journals (atom (set [])))

(defn journal-count []
  (count @journals))

(defn journal-names []
  (keys @journals))

(defn journals? []
  (< 0 (journal-count)))

(defn- scope-metric-name [raw-name]
  (let [string-name (name raw-name)]
    (string/join "." (filter identity [current-prefix string-name]))))

(defn- scope-metric-options [options]
   {
    :desc  (get options :desc)
    :units (get options :units current-units)
    :tags  (distinct
            (concat
             (get options :tags [])
             current-tags))
    })

(defn submit
  [name value & args]
  (let [metric-name    (scope-metric-name name)
        metric-options (scope-metric-options (apply hash-map args))]
    (log/trace "Submitting metric" metric-name "with value" value "and options" metric-options)
    (doseq [[journal-name journal] (seq @journals)]
      (if (not (contains? @disabled-journals journal-name))
        (do
          (journal metric-name value metric-options)
          (swap! publications inc))))
    (swap! submissions inc)))

(defn submit-values [values & args]
  (let [options (apply hash-map args)]
    (binding [current-prefix (string/join "." (filter identity [current-prefix (get options :prefix)]))
              current-units  (get options :units current-units)
              current-tags   (distinct (concat current-tags (get options :tags [])))]
      (doseq [[name value] values]
        (submit name value)))))

(defn define-journal!
  "Define a new journal."
  [name instructions]
  (swap! journals assoc name instructions))

(defn console-journal [name value options]
  (println (format "%s\t%s\t%s\t%s" (time/now) name value options)))

(def memory-journal-publications (atom []))

(defn memory-journal [name value options]
  (swap! memory-journal-publications conj [name value options]))

(defn memory-journal-publication-count []
  (count @memory-journal-publications))

(defn memory-journal-publications? []
  (< 0 (memory-journal-publication-count)))
  
(defn check-for-journals! []
  (if (not (journals?))
    (do
      (log/debug "No journals defined, defaulting to \"console\" journal.")
      (define-journal! "console" console-journal))))
