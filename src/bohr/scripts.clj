;;;; The code defining observers and journals is not part of Bohr
;;;; itself but is loaded in via Clojure scripts written using Bohr's
;;;; DSL.
;;;;
;;;; These functions load generic scripts as well as the bundled
;;;; observers & journals that come with Bohr.  See dsl.clj for more
;;;; on the DSL that scripts are written in and eval.clj for the code
;;;; which directly interprets scripts.

(ns bohr.scripts
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log])
  (:use org.satta.glob
        bohr.eval
        bohr.config))

(defn- load-script-file!
  "Load a Bohr script at the given `file` object."
  [file]
  (log/debug "Loading Bohr script at" (.getPath file))
  (try
    (eval-script-content! file)
    (catch clojure.lang.ExceptionInfo e
      (if (-> e ex-data :bohr)
        (throw
         (ex-info
          (format "(in %s) %s" (.getPath file) (.getMessage e))
          (ex-data e)))))))

(defn- load-script-directory!
  "Load a directory of Bohr scripts at `dir`.

  Only files ending with *.clj directly contained within `dir` will be
  load."
  [dir]
  (log/debug "Loading Bohr script directory at" (.getPath dir))
  (doseq [clojure-path
          (filter #(re-find #"\.clj$" %) (.list dir))]
    (load-script-file! (io/file dir clojure-path))))

(defn load-script!
  "Load a script at the given `input-path`, whether file or
  directory."
  [input-path]
  (if input-path
    (let [input (io/file input-path)]
      (cond

        ;; file doesn't exist
        (not (.exists input))
        (throw
         (ex-info
          (format "No such file or directory: %s" input-path)
          {:bohr true :type :bohr-no-such-input :path input-path}))

        (.isDirectory input) (load-script-directory! input)
        :else                (load-script-file!      input)))))

(defn load-scripts!
  "Load the scripts at the given `input-paths`, whether files or
  directories."
  [input-paths]
  (doseq [input-path input-paths]
    (load-script! input-path)))

;; Default bundled observers to load.  Will match all observers.
(def default-bundled-observers ["*/*.clj"])
  
(defn- load-bundled-observers!
  "Load observers bundled with Bohr.

  This can be controlled via the `observers` setting in the
  configuration file."
  []
  (doseq [pattern (get-config :bundled-observers default-bundled-observers)]
    (doseq [path (glob (format "%s/%s" (.getPath (io/resource "observers")) pattern))]
      (load-script! path))))

(defn- load-bundled-journals!
  "Load journals bundled with Bohr."
  []
  (doseq [pattern (get-config :bundled-journals [])]
    (doseq [path (glob (format "%s/%s" (.getPath (io/resource "journals")) pattern))]
      (load-script! path))))

(defn- load-external-scripts!
  "Load external scripts specified in configuration."
  []
  (doseq [pattern (get-config :load [])]
    (doseq [path (glob pattern)]
      (load-script! path))))

(defn populate!
  "Populate Bohr's observers and journals from the given
  `input-paths`, code pointed at by Bohr's configuration, and code
  bundled with Bohr."
  [input-paths]
  (load-bundled-observers!)
  (load-bundled-journals!)
  (load-external-scripts!)
  (load-scripts! input-paths))
