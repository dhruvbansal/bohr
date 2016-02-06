(ns bohr.log
  (:use clj-logging-config.log4j))

(def default-log-options { :log-level :info :log-pattern "%-5p %d{ISO8601} %m%n"})

(defn set-bohr-logger! [runtime-options]
  (let [default-log-level (get runtime-options :log-level)
        log-level (if (get runtime-options :verbose) :debug default-log-level)]
    (set-loggers!
     ["bohr.core" "bohr.cli" "bohr.dsl" "bohr.inputs"]
     {:pattern (get runtime-options :log-pattern) :level log-level} )))
