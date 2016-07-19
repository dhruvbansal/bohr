(ns bohr.cli-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [bohr.cli :refer :all]))

(def default-options
  {:exclude-observations []
   :config []
   :periodic false
   :verbose 0
   :include-observations []
   :once false
   :include-observers []
   :help false
   :version false
   :exclude-observers []
   })

;; From http://stackoverflow.com/questions/17314128/get-stacktrace-as-string
(defmacro with-err-str
  "Evaluates exprs in a context in which *err* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))
  
(defn- args-and-options
  [args options]
  [args
   (merge default-options options)])
  
(deftest parse-cli-test
  (with-redefs
    [exit! (constantly :exit!)]
    (testing "Command-line parsing"
      (is
       (=
        (args-and-options [] {})
        (parse-cli []))
       "on no arguments")
      (doseq [substring
              ["usage: bohr [OPTIONS] [SCRIPT ...]"
               "Options:"
               "--verbose"]
              ]
        (is
         (.contains
          (with-err-str (parse-cli ["--help"]))
          substring)
         "on --help option"))
      (is
       (=
        (System/getProperty "bohr.version")
        (string/trim-newline
         (with-out-str
           (parse-cli ["--version"]))))
       "on --version option")

      (is
       (.contains
        (with-err-str (parse-cli ["--config"]))
        "foo")))))
