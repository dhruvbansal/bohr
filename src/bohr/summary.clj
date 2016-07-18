(ns bohr.summary
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set])
  (:use table.core
        bohr.observers
        bohr.journals
        bohr.utils))

(defn- summary-row [observer-name row-name value options]
  [
   (str (get-observer observer-name :period))
   (name observer-name)
   (name row-name)
   (formatted-tags options)
   (formatted-attributes options)
   (formatted-description options)
   (formatted-value-with-units value options)
   ])

(defn- summary-table []
  (conj
   (map
    (fn [publication] (apply summary-row publication))
    @memory-journal-publications)
   (list "Period (s)", "Observer" "Observation", "Tags" "Attributes" "Description" "Value (Units)")))

(defn prepare-for-summarize! []
  (reset!
   disabled-journals
   (set/difference
    (set (journal-names))
    (set ["memory"])))
  (define-journal! "memory" memory-journal))

(defn summarize! []
  (log/debug "Producing summary")
  (if (memory-journal-publications?)
    (table (summary-table) :sort "Observation" :desc true)
    (log/info "No observers!")))
