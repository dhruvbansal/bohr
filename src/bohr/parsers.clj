(ns bohr.parsers
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defn parse-two-columns [string & {:keys [line-sep col-sep]
                            :or {line-sep #"\n" col-sep #":\s+"}}]
  (into {}
        (map
         #(string/split % col-sep 2)
         (string/split string line-sep))))
