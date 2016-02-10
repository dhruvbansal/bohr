(ns bohr.config
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clj-yaml.core :as yaml]))

(def bohr-root
  (.getCanonicalPath
   (io/file
    (.getParent (io/file *file*))
    "..")))

(defn bohr-path [& args]
  (apply
   io/file
   (concat [bohr-root] args)))

(defn bohr-resource-path [& args]
  (apply
   bohr-path
   (concat ["resources"] args)))

(def ^{:private true} bohr-config (atom {}))

(defn load-config! [runtime-options]
  (let [config-path (get runtime-options :config)]
    (if config-path
      (do
        (log/debug "Loading Bohr configuration at" config-path)
        (swap! bohr-config merge (yaml/parse-string (slurp config-path)))))))

(defn get-config [key]
  (get @bohr-config key))
