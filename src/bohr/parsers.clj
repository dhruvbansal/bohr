(ns bohr.parsers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defn parse-properties [string & {:keys [line-sep col-sep transform-raw-line converter]
                            :or {line-sep #"\n" col-sep #":\s+" transform-raw-line identity converter identity}}]
  (into {}
        (map
         (fn [line]
           (let [[name value]
                 (string/split (transform-raw-line line) col-sep 2)]
             [name (converter value)]))
         (string/split string line-sep))))

(defn parse-table [string converters & { :keys [line-sep col-sep start-at end-at transform-raw-line transform-object]
                             :or {line-sep #"\n"
                                  col-sep #",?\s+"
                                  start-at 1
                                  transform-raw-line identity
                                  transform-object identity}}]
  (map
   #(transform-object
     (into
      {}
      (filter
       identity
       (map-indexed
        (fn [index raw-value]
          (let [[name converter] (nth converters index)]
            (if name
              [name (converter raw-value)])))
        (string/split (transform-raw-line %) col-sep)))))
   (if end-at
     (subvec (string/split string line-sep) (- start-at 1) end-at)
     (subvec (string/split string line-sep) (- start-at 1)))))
