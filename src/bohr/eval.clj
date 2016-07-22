(ns bohr.eval
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as string]))

(defn- wrap-with-parentheses
  "Wraps the given `string` with parentheses.

  Turns the string contents of a Clojure script into a string version
  of a Clojure list."
  [string]
  (str "(" string ")"))

(defn- string-to-list
  "Read the given `string` as Clojure list."
  [string]
  (try
    (read-string string)
    (catch java.lang.RuntimeException e
      (throw
       (ex-info
        (format "Malformed input: %s" (.getMessage e))
        { :bohr true :type :bohr-malformed-input })))))

(defn- wrap-with-do
  "Prepends the symbol 'do into a form.
  
  This makes a list into an expression."
  [form]
  (conj form 'do))

(defn- script-namespace-for
  "Return the Clojure namespace for the given `dsl-file`.

  Just the concatenation of the string 'dsl-' with the basename of the
  file (sans extension)."
  [dsl-file]
  (symbol
   (str
    "dsl."
    (string/replace
     (last (string/split (.getPath dsl-file) #"/"))
     ".clj"
     ""))))

(defn setup-script-namespace
  "Create (or use a given `namespace`) for a script."
  ([]
   (setup-script-namespace (gensym "bohr")))
  ([namespace]
   (do 
     (binding [*ns* *ns*]
      (in-ns namespace)
      namespace))))

;; We want a namespace for when we have Bohr code loaded up in the
;; REPL.
(setup-script-namespace (symbol 'bohr.repl))

(defn- eval-in-namespace
  "Evaluate a form in the given namespace.

  Imports the various parts of Bohr needed to implement the DSL."
  [namespace form]
  (binding [*ns* *ns*]
    (in-ns namespace)
    (refer-clojure)
    (require '[clojure.tools.logging :as log])
    (require '[clojure.string :as string])
    (require '[clj-time.core :as time])
    (require '[clj-time.format :as time-format])
    (require '[clojure.java.io :as io])
    (use 'clojure.java.shell)
    (use 'bohr.config)
    (use 'bohr.observers)
    (use 'bohr.journals)
    (use 'bohr.dsl.macros)
    (use 'bohr.dsl.os)
    (use 'bohr.dsl.parsers)
    (use 'bohr.dsl.helpers)
    (use 'bohr.dsl.schedules)
    (eval form)))

(defn eval-script-content!
  "Evaluate the contents of the given `dsl-file`.

  Will create a namespace for the file based on its basename."
  [dsl-file]
  (log/trace "Evaluating script at" (.getPath dsl-file))
  (eval-in-namespace
   (script-namespace-for dsl-file)
   (wrap-with-do
    (string-to-list
     (wrap-with-parentheses
      (slurp dsl-file))))))
