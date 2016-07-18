;;;; This namespace provides some utility fuctions.

(ns bohr.utils
  (:require [clojure.string :as string])
  (:use bohr.observers))

(defn formatted-period [observer-name]
  (let [period (get-observer observer-name :period)]
    (if period
      (format "%ds" period)
      "")))
      
(defn formatted-value [value]
  (cond
    (float? value)
    (format "%.3f" value)
    
    :else (str value)))

(defn formatted-value-with-units [value options]
  (format
   "%s %s"
   (formatted-value value)
   (or
    (:units options)
    " ")))

(defn formatted-tags [options]
  (string/join
   ","
   (sort
    (seq
     (or
      (:tags options)
      [])))))

(defn formatted-attributes [options]
  (string/join
   ","
   (map
    (fn [[k,v]]
      (format "%s:%s"
              (name k)
              v))
    (seq
     (or (:attributes options)
         {})))))

(defn formatted-description [options]
  (or (:desc options) ""))
