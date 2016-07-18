;;;; Defines the command-line interface (CLI) for the `bohr` command.
;;;;
;;;; The `clojure.tools.cli` library is used behind the scenes.

(ns bohr.cli
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:use     [clojure.string :only [join]]))

(defn- option-incrementer
  "Increments the current value of a counter-like option.

  Intended to be used with the `parse-opts` function (hence the unused
  `current-option-value` argument)."
  [current-options current-option-id current-option-value]
  (update-in current-options [current-option-id] inc))

(defn- option-appender
  "Appends a value to a list-like option.

  Intended to be used with the `parse-opts` function."
  [current-options current-option-id current-option-value]
  (update-in current-options [current-option-id] conj current-option-value))

;; Options for the command-line parser.
(def  ^{:private true} cli-parser-options
  [
   ["-v" "--verbose"    "Log DEBUG statements (repeat for TRACE)." :default 0 :assoc-fn option-incrementer]

   ["-c" "--config PATH" "Read configuration file/dir at the given path.  Can be given more than once."        :default [] :assoc-fn option-appender]
   
   ["-X" "--exclude-observer PATTERN" "Don't run observers with matching names.  Can be given more than once." :default [] :assoc-fn option-appender :id :exclude-observers] ; pluralize :id to match configuration file syntax
   ["-I" "--include-observer PATTERN" "Only run observers with matching names.  Can be given more than once."  :default [] :assoc-fn option-appender :id :include-observers] ; pluralize :id to match configuration file syntax

   ["-x" "--exclude-observation PATTERN" "Don't submit observations with matching names.  Can be given more than once." :default [] :assoc-fn option-appender :id :exclude-observations] ; pluralize :id to match configuration file syntax
   ["-i" "--include-observation PATTERN" "Only submit observations with matching names.  Can be given more than once."  :default [] :assoc-fn option-appender :id :include-observations] ; pluralize :id to match configuration file syntax
   
   ["-h" "--help"       "Print this help"                                          :default false]
   ["-o" "--once"       "Submit all reports, once"                                 :default false]
   ["-p" "--periodic"   "Run continuously, periodically submitting reports"        :default false]

   ["-V" "--version"    "Print version and exit"                                   :default false]
   ])

(defn- usage
  "Returns the usage text for the `bohr` program.

  The argument `options-summary` is returned by the `parse-opts`
  function."
  [options-summary]
  (->> ["usage: bohr [OPTIONS] [SCRIPT ...]

Bohr is a scientist who observes your system and takes many
readings. He periodically writes reports which he submits to several
journals.  These journals write them to data stores or other services.

When run without any arguments

  $ bohr

Bohr will observe a standard set of system metrics (CPU, memory, disk,
&c.) and summarize his findings in a table pretty-printed to console.

When run with the --once (or -o) flag

  $ bohr --once
  $ bohr -o

Bohr will make observations and submit them to journals ONCE, instead
of just printing a summary, as above.

When run with the --periodic (or -p) flag

  $ bohr --periodic
  $ bohr -p

Bohr will run continuously, periodically making observations and
submitting them to journals.

You can define your own observations for Bohr to make and journals for
Bohr to submit to by writing Clojure scripts in Bohr's DSL.  You can
pass this scripts directly to Bohr

  $ bohr my_observer.clj my_journal.clj ...

Bohr will also read the above configuration (as well as more) from a
configuration file or directory

  $ bohr --config /etc/bohr/bohr.yml --config /etc/bohr/conf.d"
        ""
        "Options:"
        options-summary
        ""]
       (join \newline)))

(defn exit!
  "Exit the `bohr` program, returning the given `status` (default 0)
  and (optionally) printing the given `msg`."
  ([]
   (System/exit 0))
  ([status]
   (System/exit status))
  ([status msg]
   (if msg (.println *err* msg))
   (System/exit status)))

(defn parse-cli
  "Parse the command-line and return arguments and options.

  If the `--help` option was present, print out a usage summary and
  exit.
  
  If the `--version` option was present, print out the current Bohr
  version and exit.

  If any errors were encountered during parsing, log them and
  exit.

  Otherwise, return a 2-element vector consisting of the parsed
  command-line arguments and options."
  [cli-args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts cli-args cli-parser-options)]

    (cond
      (:help options)
      (exit! 1 (usage summary))

      (:version options)
      (do
        (println (System/getProperty "bohr.version"))
        (exit!))
      
      errors
      (do
        (doseq [error errors]
          (.println *err* error))
        (exit! 2))
      
      :else
      [arguments options])))
