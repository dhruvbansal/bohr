(ns bohr.helpers
  (:require [clojure.string :as string])
  (:use clojure.java.shell))
  
(defn sh-output [& args]
  (string/trim (get (apply sh args) :out)))
