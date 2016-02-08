(ns bohr.scripts
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:use bohr.eval))

(defn- read-script! [file]
  (log/debug "Loading Bohr script at" (.getPath file))
  (try
    (eval-script-content! (slurp file))
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (throw
         (ex-info
          (format "(in %s) %s" (.getPath file) (.getMessage e))
          (ex-data e)))))))

(defn- read-script-directory! [dir]
  (log/debug "Loading Bohr script directory at" (.getPath dir))
  (doseq [clojure-path
          (filter identity (.list dir))]
    (read-script! (io/file dir clojure-path))))

(defn- read-input! [input-path]
  (if input-path
    (let [input (io/file input-path)]
      (cond

        ;; file doesn't exist
        (not (.exists input))
        (throw
         (ex-info
          (format "No such file or directory: %s" input-path)
          {:bohr true :type :bohr-no-such-input :path input-path}))

        (.isDirectory input) (read-script-directory! input)
        :else                (read-script!           input)))))

(defn read-inputs! [input-paths]
  (doseq [input-path input-paths]
    (read-input! input-path)))
