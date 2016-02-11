(ns bohr.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log])
  (:use     [clojure.string :only [join]]
            bohr.log))

(defn- option-incrementer [current-options current-option-id current-option-value]
  (update-in current-options [current-option-id] inc))

(defn- option-appender [current-options current-option-id current-option-value]
  (update-in current-options [current-option-id] conj current-option-value))
  
(def  ^{:private true} default-cli-options { :loop false })

(def  ^{:private true} cli-parser-options
  [
   ["-h" "--help"       "Print this help"                          :default false]
   ["-v" "--verbose"    "Log DEBUG statements (repeat for TRACE)." :default 0 :assoc-fn option-incrementer]

   ["-X" "--exclude-observer PATTERN" "Don't run observers with matching names.  Can be given more than once." :default [] :assoc-fn option-appender]
   ["-I" "--include-observer PATTERN" "Only run observers with matching names.  Can be given more than once."  :default [] :assoc-fn option-appender]

   ["-c" "--config PATH" "Read configuration file/dir at the given path.  Can be given more than once." :default [] :assoc-fn option-appender ]

   ["-l" "--loop"       "Run continuously, updating all TTLs"      :default false]
   ])

(defn- usage [options-summary]
  (->> ["usage: bohr [options] SCRIPT [SCRIPT ...]

Bohr is a scientist who observes your system and takes many
configurable readings.  He writes reports which he submits to several
journals.  These journals write them to data stores or other services.

Define observations for Bohr to make, reports for Bohr to write, and
journals for Bohr to submit to via Clojure scripts in Bohr's DSL.

  ;;; in bohr_scripts/test.clj

  ;; Define static, *compile-time* observations.
  (static :my.static
    ;; Write arbitrary code here...
    (rand))

  ;; Define *runtime* observations that run once
  (oberve :my.once
    ;; Write arbitrary code here...
    (rand))

  ;; Define *runtime* observations that run periodically.
  (observe :my.periodic ttl: 5
    ;; Write arbitrary code here...
    (rand))

  ;; Perform intermediate calculations.
  (calc :my.intermediate
    ;; Arbitrary code here can refer to (current) values of
    ;; any previously defined observations.
    (* (& :my.static) (& :my.once) (& :my.periodic))

  ;; Submit reports that run either once or periodically, as above.
  ;; Reports can refer to the (current) values of observations &
  ;; calculations, as above.
  (report \"test\" ttl: 10
    (submit \"metric.first\"  (& :my.intermediate))
    (submit \"metric.second\" (* 2 (& :my.intermediate))))

Bohr's default behavior is to evaluate all scripts passed to it and
then perform all observations, make all calculations, and submit all
reports *once* and exit.

  $ bohr bohr_scripts/test.clj
  $ bohr bohr_scrips

In `--loop' mode, Bohr will run forever, continuously performing
observations, making calculations, and submitting reports till he
dies.  Silly guy."
        ""
        "Options:"
        options-summary
        ""]
       (join \newline)))

(defn- log-errors [errors]
  (doseq [error errors]
    (log/error error)))

(defn exit
  ([status]
   (System/exit status))
  ([status msg]
   (if msg (.println *err* msg))
   (System/exit status)))

(defn parse-cli [cli-args]
  (let [{:keys [options arguments errors summary]} (parse-opts cli-args cli-parser-options)
        runtime-options (merge default-log-options default-cli-options options)]
    ;; set logger here b/c we can use it for errors
    (set-bohr-logger! runtime-options)
    (cond
      (:help runtime-options) (exit 1 (usage summary))
      errors                  (exit 2 (log-errors errors))
      :else                   [arguments runtime-options])))
