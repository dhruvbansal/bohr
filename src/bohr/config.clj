;;;; Bohr's configuration is a global map of values.
;;;;

(ns bohr.config
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml]))

;; Bohr configuration.
(def ^{:private true} bohr-config (atom {:bohr {} }))

(defn- read-config-file
  "Return the parsed contents of the given YAML `file` handle."
  [file]
  (log/debug "Reading Bohr configuration at" (.getPath file))
  (yaml/parse-string (slurp file)))

(defn- read-config-directory
  "Read all YAML configuration files in the given `dir`.

  Does not recurse through the directory."
  [dir]
  (log/debug "Reading Bohr configuration directory at" (.getPath dir))
  (map
   #(read-config-file (io/file dir %))
   (filter
    #(re-find #"\.ya?ml" %)
    (.list dir))))

(defn- read-config
  "Returns configuration from the given `path` which may be either a
  file or a directory."
  [path]
  (let [file (io/file path)]
    (cond
      (not (.exists file))
      (throw
       (ex-info
        (format "No such file or directory: %s" path)
        {:bohr true :type :bohr-no-such-input :path path}))

      (.isDirectory file) (read-config-directory file)
      :else               (read-config-file file))))

(defn merge-configs
  "Return the merged version of all the `configs`.

  Tries to be intelligent about merging containers such as maps and
  sequences vs 'objects' such as strings, numbers, &c."
  [configs]
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

(defn load-config!
  "Load configuration from the given `cli-options`.

  The `cli-options` map should have a key `config` with the path
  to a YAML configuration file or directory as its corresponding
  value.

  The `cli-options` map will itself be merged into the
  configuration."
  [cli-options]
  (let [config-paths (or (get cli-options :config) [])]
    (reset!
     bohr-config
     (merge-configs
      (flatten
       (concat
        [@bohr-config cli-options]
        (map read-config config-paths)))))))

(defn get-config
  "Return the value of the configuration `key`."
  ([key]
   (get @bohr-config key))
  ([key default]
   (get @bohr-config key default)))
