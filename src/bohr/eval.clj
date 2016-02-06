(ns bohr.eval
  (:require [clojure.tools.logging :as log]))

(defn- wrap-with-parentheses [string]
  (str "(" string ")"))

(defn- string-to-list [string]
  (try
    (read-string string)
    (catch java.lang.RuntimeException e
      (throw
       (ex-info
        (format "Malformed input: %s" (.getMessage e))
        { :bohr true :type :bohr-malformed-input })))))

(defn- wrap-with-do [block]
  (conj block 'do))

(defn- setup-script-namespace []
  (binding [*ns* *ns*]
      (in-ns 'bohr.script)
      (refer-clojure)
      (use 'bohr.observers)
      (use 'bohr.notebook)
      (use 'bohr.journals)
      (use 'bohr.dsl)))

(defn- eval-in-script-namespace [form]
  (setup-script-namespace)
  (binding [*ns* *ns*]
    (in-ns 'bohr.script)
    (refer-clojure)
    (eval form)))

(defn eval-script-content! [dsl-string]
  (log/trace "Evaluating script")
  (-> dsl-string wrap-with-parentheses string-to-list wrap-with-do eval-in-script-namespace))
