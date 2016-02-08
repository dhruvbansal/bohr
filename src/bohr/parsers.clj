(ns bohr.parsers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defn parse-two-columns [string & {:keys [line-sep col-sep value-function]
                            :or {line-sep #"\n" col-sep #":\s+" value-function identity}}]
  (into {}
        (map
         (fn [line]
           (let [[name value]
                 (string/split line col-sep 2)]
             [name (value-function value)]))
         (string/split string line-sep))))
