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

(defn- uberjar?
  "Is Bohr currently running from an lein uberjar?"
  ;; If the 'resources/observers' directory exists on disk, then we
  ;; are NOT running from within an uberjar.
  []
  (-> "observers" io/resource .getPath io/file .exists not))

(def ^:private running-jar 
  "Resolves the path to the current running jar file."
  (-> :keyword class (.. getProtectionDomain getCodeSource getLocation getPath)))

(defn- list-resources-in-jar
  "Return a list of all resources in the currently running jar."
  []
  (let [jar (java.util.jar.JarFile. running-jar)
        entries (.entries jar)]
    (loop [result  []]
      (if (.hasMoreElements entries)
        (recur (conj result (.. entries nextElement getName)))
        result))))

(def ^:private resources-dir
  "The local directory containing resources (if not running as an uberjar)."
  (-> "observers" io/resource .getPath io/file .getParentFile))

(defn- list-resources-on-disk
  "Return a list of all resources available as files on disk."
  []
  (map
   (fn [file]
     (subs
      (.getPath file)
      (+ 1
         (-> resources-dir .getPath .length))))
   (filter
    (fn [file]
      (not
       (= resources-dir file)))
    (file-seq resources-dir))))

(defn- list-resources
  "Return a list of resources, from disk or uberjar as appropriate."
  []
  (if (uberjar?)
    (list-resources-in-jar)
    (list-resources-on-disk)))

(defn- load-matching-bundled-scripts!
  "Loads each script from the bundled resources matching the
  pattern-string."
  [pattern-strings]
  (let [bundled-resources (list-resources)]
    (doseq [pattern-string pattern-strings]
      (let [pattern (re-pattern pattern-string)]
        (doseq [bundled-resource bundled-resources]
          (if (re-find pattern bundled-resource)
            (load-script-file! (io/resource bundled-resource))))))))

;; Default bundled observers to load.  Will match system observers and
;; the Bohr observer.
(def default-bundled-observers ["bohr", "system/.*"])

;; Default bundled journals to load.  Will match no journals.
(def default-bundled-journals [])

(defn- load-bundled-observers!
  "Load observers bundled with Bohr.

  Uses the `bundled-observers` setting in the configuration file."
  []
  (load-matching-bundled-scripts!
   (map
    (fn [pattern-string]
      (format "observers/%s\\.clj" pattern-string))
    (get-config :observers default-bundled-observers))))

(defn- load-bundled-journals!
  "Load journals bundled with Bohr.

  Uses the `bundled-journals` setting in the configuration file."
  []
  (load-matching-bundled-scripts!
   (map
    (fn [pattern-string]
      (format "journals/%s\\.clj" pattern-string))
    (get-config :journals default-bundled-journals))))

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
