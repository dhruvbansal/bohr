(ns bohr.journals
  (:require [clj-time.core :as time]
            [clojure.tools.logging :as log]))

(def ^{:private true} journals (atom {}))

(defn- submit-all!
  "Submit the metric to all journals."
  [name value options]
  (log/trace "Submitted metric" name "with value" value "and options" options)
  (doseq [[_ journal] (seq @journals)]
    (journal name value options)))
  
(defn submit-with-observer!
  "Submit the metric from within the context of the given observation."
  [observer name value options]
  (submit-all!
   (if (get observer :prefix)
     (format "%s.%s" (get observer :prefix) name)
     name)
   value
   {
    :desc  (get options :desc  (get observer :desc))
    :units (get options :units (get observer :units))
    :tags  (distinct
            (concat
             (get observer :tags [])
             (get options :tags [])))
    }))

(defn define-journal!
  "Define a new journal."
  [name instructions]
  (swap! journals assoc name instructions))

(define-journal! :console
  (fn [name value options]
    (println (format "%s\t%s\t%s\t%s" (time/now) name value options))))
