;;;; Contains helper functions intended to be used from within the
;;;; Bohr DSL.

(ns bohr.helpers
  (:require [clojure.string  :as string]
            [clojure.java.io :as io])
  (:use clojure.java.shell
        bohr.observers
        bohr.journals))

(defn sh-output
  "Returns the (newline-trimmed) output of a shell `command` or
  pipeline."
  [command]
  (string/trim (get (sh "bash" "-c" command) :out)))

;; The directory containing the /proc filesystem.
;;
;; Should only be accessed on Linux systems which implement the /proc
;; filesystem, but harmless to define it here.
(def procfile-dir (io/file "/proc"))

(defn procfile-contents
  "Return the (newline-trimmed) contents of the procfile at the given
  path, relative to the `procfile-dir`.

  Given the `procfile-dir` is /proc, the procfile /proc/stat can be
  referred to via the path `stat`."
  [path]
  (string/trim
   (slurp
    (java.io.FileReader.
     (io/file procfile-dir path)))))

(defn sysctl
  "Return the value of the sysctl variable `name`."
  [name]
  (sh-output (format "sysctl -n '%s'" name)))

(defn translate [source mapping]
  (into
   {}
   (map
    (fn [[source_name target_name]] [target_name (get source source_name)])
    (seq mapping))))

(defn annotate [values annotations]
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
