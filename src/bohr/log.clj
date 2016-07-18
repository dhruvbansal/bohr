;;;; Sets the Bohr logger.
;;;;
;;;; Log4j is used behind the scenes with some Clojure
;;;; libraries (clj-logging-config) wrapping it.

(ns bohr.log
  (:use clj-logging-config.log4j
        bohr.config))

;; Default options for the Bohr logger.
;;
;; FIXME Currently no way to change these (though `log-level` can be
;; overriden via the `verbose` command-line flag).
(def default-log-options { :log-level :info :log-pattern "%-5p %d{ISO8601} [%-12c{1}] %m%n"})

(defn set-bohr-logger!
  "Sets the root Bohr logger.

  Argument `cli-options` is a map with keys:
  
  - :verbose -- increases runtime log-level, 0=info, 1=debug, 2=trace"
  [cli-options]
  (let [verbosity (get cli-options :verbose)
        log-level (cond
                    (or (nil? verbosity) (= 0 verbosity)) (get default-log-options :log-level)
                    (=  1 verbosity) :debug
                    (<= 2 verbosity) :trace)]
    (set-loggers! :root
                 {:pattern (get default-log-options :log-pattern)
                  :level log-level
                  :out *err*})))
