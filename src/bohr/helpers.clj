(ns bohr.helpers
  (:require [clojure.string  :as string]
            [clojure.java.io :as io])
  (:use clojure.java.shell))
  
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
