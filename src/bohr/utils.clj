;;;; This namespace provides some utility fuctions.

(ns bohr.utils
  (:require [clojure.string :as string]))

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

(defn allowed?
  "Is the object with the given `object-name` allowed,
  given the `excluded-patterns` and `included-patterns`?"
  [object-name excluded-patterns included-patterns]
  (cond
    (and
     (empty? excluded-patterns)
     (empty? included-patterns))
    true

    (and
     (not-empty excluded-patterns)
     (empty? included-patterns))
    (not-any?
     #(re-find % (name object-name))
     excluded-patterns)

    (and
     (empty? excluded-patterns)
     (not-empty included-patterns))
    (some
     #(re-find % (name object-name))
     included-patterns)

    :else
    (and
     (some
      #(re-find % (name object-name))
      included-patterns)
     (not-any?
      #(re-find % (name object-name))
      excluded-patterns))))
