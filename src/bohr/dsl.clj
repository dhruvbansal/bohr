(ns bohr.dsl
  (:require [clojure.tools.logging :as log]))

(defn- wrap-with-parentheses [string]
  (str "(" string ")"))

(defn- wrap-with-do-and-namespace [block]
  (conj block '(in-ns 'bohr.core) 'do))

(defn- string-to-list [string]
  (try
    (read-string string)
    (catch java.lang.RuntimeException e
      (throw
       (ex-info
        (format "Malformed input: %s" (.getMessage e))
        { :bohr true :type :bohr-malformed-input })))))

(defn dsl-eval-string!
  "Evaluate string as Bohr DSL."
  [dsl-string]
  (log/trace "Evaluating DSL")
  (eval (wrap-with-do-and-namespace (string-to-list (wrap-with-parentheses dsl-string)))))

