(ns bohr.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log])
  (:use     [clojure.string :only [join]]
            bohr.log))

(def  ^{:private true} default-cli-options { :daemon false })

(def  ^{:private true} cli-parser-options
  [
   ["-h" "--help" "Print this help" :default false]
   ["-i" "--input PATH" "Input path(s) to read"]
   ["-d" "--daemon" "Run continuously" :default false]
   ["-v" "--verbose" "Print DEBUG statements" :default false]
   ])

(defn- usage [options-summary]
  (->> ["usage: bohr [options]

Bohr is a scientist who observes your system and takes many
configurable readings.  He writes reports which he submits to several
journals.  These journals write them to data stores or other services.

Define observations for Bohr to make, reports for Bohr to write, and
journals for Bohr to submit to via Clojure scripts in Bohr's DSL (the
`--input' option).

When called without any arguments, Bohr's default behavior is to run
all reports and print them to standard output.  In `--daemon' mode,
Bohr will run forever, continuously making observations and submitting
reports till he dies.  Silly guy."
        ""
        "Options:"
        options-summary
        ""]
       (join \newline)))

(defn- log-errors [errors]
  (doseq [error errors]
    (log/error error)))

(defn exit [status msg]
  (if msg (.println *err* msg))
  (System/exit status))

(defn parse-cli [cli-args]
  (let [{:keys [options arguments errors summary]} (parse-opts cli-args cli-parser-options)
        runtime-options (merge options default-cli-options default-log-options)]
    ;; set logger here b/c we can use it for errors
    (set-bohr-logger! runtime-options)
    (cond
      (:help runtime-options) (exit 1 (usage summary))
      errors                  (exit 2 (log-errors errors))
      :else                   runtime-options)))
