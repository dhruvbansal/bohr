(ns bohr.summary
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [clojure.string :as string])
  (:use table.core
        bohr.observers
        bohr.journals))

(defn- formatted-ttl [observer-name]
  (let [ttl (get-observer observer-name :ttl)]
    (if ttl
      (format "%d" ttl)
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

(defn- summary-row [observer-name row-name value options]
  [
   (name row-name)
   (formatted-value-with-units value options)
   (or (get options :desc) "")
   (formatted-ttl observer-name)
   (formatted-tags (get options :tags))
   ])

(defn- summary-table []
  (conj
   (map
    (fn [publication] (apply summary-row publication))
    @memory-journal-publications)
   (list "Name" "Value (units)" "Description" "TTL", "Tags")))

(defn prepare-for-summarize! [runtime-options]
  (reset!
   disabled-journals
   (set/difference
    (set (journal-names))
    (set ["memory"])))
  (define-journal! "memory" memory-journal))

(defn summarize! [runtime-options]
  (log/debug "Producing summary")
  (if (memory-journal-publications?)
    (table (summary-table) :sort true :desc true)
    (log/info "No metrics submitted")))
