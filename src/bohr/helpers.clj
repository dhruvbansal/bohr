(ns bohr.helpers
  (:require [clojure.string  :as string]
            [clojure.java.io :as io])
  (:use clojure.java.shell
        bohr.observers
        bohr.journals))

(defn sh-output [command]
  (string/trim (get (sh "bash" "-c" command) :out)))

(def procfile-dir (io/file "/proc"))

(defn procfile-contents [path]
  (string/trim
   (slurp
    (java.io.FileReader.
     (io/file procfile-dir path)))))

(defn sysctl [name]
  (sh-output (format "sysctl -n '%s'" name)))

(defmacro case-os [& clauses]
  `(case (& :os.type)
     ~@clauses
     (log/error "Cannot observe" current-observer "for OS" (& :os.type))))

(defn translate [source mapping]
  (into
   {}
   (map
    (fn [[source_name target_name]] [target_name (get source source_name)])
    (seq mapping))))

(defn annotate-values [values annotations]
  (into
   {}
   (map
    (fn [[key value]]
      (let [annotation (get annotations key)]
        [key {:value value
              :desc  (get annotation :desc)
              :units (get annotation :units)
              :tags  (get annotation :tags)
              }]))
    (seq values))))
