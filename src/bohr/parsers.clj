(ns bohr.parsers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:use bohr.helpers))

(defn- apply-converter [converter value]
  (cond
    (= :string  converter) value
    (= :integer converter) (Integer/parseInt value)
    (= :long    converter) (Long/parseLong value)
    (= :float   converter) (Float/parseFloat value)
    (= :double  converter) (Double/parseDouble value)
    (integer?   converter) (* converter (Long/parseLong value))
    (float?     converter) (* converter (Float/parseFloat value))
    :else                  (converter value)))
    
(defn- extract-lines [string line-sep start-at end-at]
  (if end-at
    (subvec (string/split string line-sep) (- start-at 1) end-at)
    (subvec (string/split string line-sep) (- start-at 1))))

(defn- parse-properties-line [raw-line transform-raw-line col-sep converter]
  (let [[property-name property-value]
        (string/split (transform-raw-line raw-line) col-sep 2)]
    [property-name (apply-converter converter property-value)]))

(defn- perhaps-translate [source mapping]
  (if mapping
    (translate source mapping)
    source))

(defn parse-properties [string & {:keys [line-sep start-at end-at
                                         transform-raw-line col-sep converter
                                         translation]
                            :or {line-sep #"\n"
                                 start-at 1
                                 col-sep #":\s+"
                                 transform-raw-line #(string/trim %)
                                 converter identity}}]

  (perhaps-translate
   (into
    {}
    (map
     #(parse-properties-line % transform-raw-line col-sep converter)
     (extract-lines string line-sep start-at end-at)))
   translation))

(defn- parse-table-line [raw-line transform-raw-line col-sep column-count]
  (if column-count
    (string/split
     (transform-raw-line raw-line)
     col-sep
     column-count)
    (string/split
     (transform-raw-line raw-line)
     col-sep)))
    
(defn- map-table-line [raw-values converters]
  (into
   {}
   (filter
    identity
    (map-indexed
     (fn [index raw-value]
       (let [[name converter]
             (nth converters index)]
         (if name
           [name (apply-converter converter raw-value)])))
     raw-values))))

(defn parse-table [string converters & { :keys [line-sep start-at end-at transform-raw-line col-sep transform-row row-filter column-count]
                             :or {line-sep #"\n"
                                  start-at 1
                                  transform-raw-line #(string/trim %)
                                  col-sep #",?\s+"
                                  transform-row identity
                                  row-filter identity}}]

  (map
   transform-row
   (filter
    row-filter
    (map
     (fn [raw-line]
       (map-table-line
        (parse-table-line raw-line transform-raw-line col-sep column-count)
        converters))
     (extract-lines string line-sep start-at end-at)))))

(defn map-table [via table & { :keys [delete-key transform-row]
                             :or {delete-key false
                                  transform-row identity}}]
  
  (into
   {}
   (map
    (fn [row]
      [(get row via)
       (transform-row
        (if delete-key
          (dissoc row via)
          row))])
    table)))
