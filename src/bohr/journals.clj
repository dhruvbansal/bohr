(ns bohr.journals
  (:require [clj-time.core :as time]))

(def ^{:private true} journals (atom {}))

(defn submit!
  "Submit the record to all journals."
  [name value options]
  (doseq [[journal-name journal] (seq @journals)]
    (apply journal name value options)))

(defn define-journal!
  "Define a new journal."
  [name instructions]
  (swap! journals assoc name instructions))

(define-journal! "console"
  (fn [name value options]
    (println (format("%s\t%s\t%s\t%s" (time/now) name value options)))))
