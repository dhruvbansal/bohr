(ns bohr.summary
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [clojure.string :as string])
  (:use table.core
        bohr.journals))

(defn- summary-value [value]
  (cond
    (float? value)
    (format "%.3f" value)
    
    :else (str value)))

(defn- summary-tags [tags]
  (string/join "," (seq tags)))

(defn- summary-row [row-name value options]
  [
   (format "%s" (name row-name))
   (format "%s %s" (summary-value value) (or (get options :units) " "))
   (format "%s" (or (get options :desc) ""))
   (format "%s" (summary-tags (get options :tags)))
   ])

(defn- summary-table []
  (conj
   (map
    (fn [publication] (apply summary-row publication))
    @memory-journal-publications)
   (list "Name" "Value (units)" "Description" "Tags")))

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
