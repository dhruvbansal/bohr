(ns bohr.inputs
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:use bohr.dsl))

(defn- read-input! [file]
  (log/debug "Loading Bohr input at" (.getPath file))
  (dsl! (slurp file)))

(defn read-inputs! [input-path]
  (if input-path
    (let [input (io/file input-path)]
      (cond

        ;; file doesn't exist
        (not (.exists input))
        (throw
         (ex-info
          (format "No such file or directory: %s" input-path)
          {:bohr true :type :bohr-no-such-input :path input-path}))

        ;; file is directory
        (.isDirectory input)
        (do
          (log/debug "Loading Bohr input directory at" input-path)
          (doseq [clojure-path
                  (filter identity (.list input))]
            (read-input! (io/file input clojure-path))))

        ;; file is file
        :else (read-input! input)))))
