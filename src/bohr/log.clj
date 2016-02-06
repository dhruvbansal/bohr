(ns bohr.log
  (:use clj-logging-config.log4j))

(def default-log-options { :log-level :info :log-pattern "%-5p %d{ISO8601} %m%n"})

(defn set-bohr-logger! [runtime-options]
  (let [default-log-level (get runtime-options :log-level)
        verbosity         (get runtime-options :verbose)
        log-level (cond
                    (or (nil? verbosity) (= 0 verbosity)) default-log-level
                    (=  1 verbosity) :debug
                    (<= 2 verbosity) :trace)]
    (set-loggers!
     ["bohr.core" "bohr.cli" "bohr.dsl" "bohr.inputs" "bohr.notebook" "bohr.observers" "bohr.journals"]
     {:pattern (get runtime-options :log-pattern) :level log-level} )))
