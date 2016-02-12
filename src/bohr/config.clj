(ns bohr.config
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml]))

(def ^{:private true} bohr-config (atom {:bohr {} }))

(defn- read-config-file [file]
  (log/debug "Reading Bohr configuration at" (.getPath file))
  (yaml/parse-string (slurp file)))

(defn- read-config-directory [dir]
  (log/debug "Reading Bohr configuration directory at" (.getPath dir))
  (map
   #(read-config-file (io/file dir %))
   (filter
    #(re-find #"\.ya?ml" %)
    (.list dir))))

(defn- read-config-path [path]
  (let [file (io/file path)]
    (cond
      (not (.exists file))
      (throw
       (ex-info
        (format "No such file or directory: %s" path)
        {:bohr true :type :bohr-no-such-input :path path}))

      (.isDirectory file) (read-config-directory file)
      :else               (read-config-file file))))

(defn- merge-configs [configs]
  (apply
   merge-with
   (fn [earlier-value later-value]
     (cond
       (and
        (map? earlier-value)
        (map? later-value))
       (merge earlier-value later-value)
       
       (and
        (seq? earlier-value) (seq? later-value))
       (concat earlier-value later-value)
       
       :else later-value))
   configs))

(defn load-config! [runtime-options]
  (let [config-paths (or (get runtime-options :config) [])]
    (reset!
     bohr-config
     (merge-configs
      (flatten
       (concat
        [@bohr-config]
        (map read-config-path config-paths)))))))

(defn get-config [key]
  (get @bohr-config key))
