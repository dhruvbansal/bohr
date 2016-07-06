(ns bohr.summary
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [clojure.string :as string])
  (:use table.core
        bohr.observers
        bohr.journals))

(defn- formatted-period [observer-name]
  (let [period (get-observer observer-name :period)]
    (if period
      (format "%ds" period)
      "")))
      
(defn- formatted-value [value]
  (cond
    (float? value)
    (format "%.3f" value)
    
    :else (str value)))

(defn formatted-value-with-units [value options]
  (format
   "%s %s"
   (formatted-value value)
   (or (get options :units) " ")))

(defn- formatted-tags [tags]
  (string/join "," (sort (seq tags))))

(defn- formatted-attributes [attributes]
  (string/join
   ","
   (map
    (fn [[k,v]] (format "%s:%s" (name k) v))
    (seq attributes))))

(defn- summary-row [observer-name row-name value options]
  [
   (str (get-observer observer-name :period))
   (name observer-name)
   (name row-name)
   (formatted-tags (get options :tags []))   
   (formatted-attributes (get options :attributes {}))
   (or (get options :desc) "")   
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
