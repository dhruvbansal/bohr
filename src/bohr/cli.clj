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

;; Default command-line options.
(def  ^{:private true} default-cli-options { :loop false })

;; Options for the command-line parser.
(def  ^{:private true} cli-parser-options
  [
   ["-v" "--verbose"    "Log DEBUG statements (repeat for TRACE)." :default 0 :assoc-fn option-incrementer]

   ["-c" "--config PATH" "Read configuration file/dir at the given path.  Can be given more than once."        :default [] :assoc-fn option-appender]
   ["-X" "--exclude-observer PATTERN" "Don't run observers with matching names.  Can be given more than once." :default [] :assoc-fn option-appender :id :exclude-observers] ; pluralize :id to match configuration file syntax
   ["-I" "--include-observer PATTERN" "Only run observers with matching names.  Can be given more than once."  :default [] :assoc-fn option-appender :id :include-observers] ; pluralize :id to match configuration file syntax

   ["-h" "--help"       "Print this help"                                          :default false]
   ["-s" "--submit"     "Submit reports to journals instead of just printing them" :default false]
   ["-l" "--loop"       "Run continuously instead of just once"                    :default false]
   ["-V" "--version"    "Print version and exit"                                   :default false]
   ])

(defn- usage
  "Returns the usage text for the `bohr` program.

  The argument `options-summary` is returned by the `parse-opts`
  function."
  [options-summary]
  (->> ["usage: bohr [options] [SCRIPT ...]

Bohr is a scientist who observes your system and takes many
readings. He periodically writes reports which he submits to several
journals.  These journals write them to data stores or other services.

When run without any arguments

  $ bohr

Bohr will observe a standard set of system metrics (CPU, memory, disk,
&c.) and submit a single round of reports which will be printed to
console.

Define your own observations for Bohr to make via Clojure scripts in
Bohr's DSL.  You can pass these scripts to Bohr directly:

  $ bohr my_observer.clj ...

Bohr will make your observation in addition to his usual set of system
metrics.

When run with the --submit flag

  $ bohr --submit

Bohr will submit his reports to journals, instead of just printing
them to the console.  You can provide additional journals on the
command line

  $ bohr my_journal.clj --submit

This can, of course, be combined with additional observers, too:

  $ bohr my_observer.clj my_journal.clj --submit

When the `--loop' flag is passed, Bohr will run forever, continuously
performing observations and submitting reports (`--loop` implies
`--submit`) till he dies.  Silly guy."
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
  (let [{:keys [options arguments errors summary]} (parse-opts cli-args cli-parser-options)
        runtime-options (merge default-cli-options options)]
    
    (cond
      (:help runtime-options)
      (exit! 1 (usage summary))

      (:version runtime-options)
      (do
        (println (System/getProperty "bohr.version"))
        (exit!))
      
      errors
      (do
        (doseq [error errors]
          (.println *err* error))
        (exit! 2))
      
      :else
      [arguments runtime-options])))
