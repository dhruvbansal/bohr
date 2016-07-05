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
  (:use bohr.eval
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

(defn- load-script!
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

(defn- load-bundled-observer!
  "Load the script at the given `path` relative to the bundled
  observers directory."
  [path]
  (load-script-file!
   (io/resource
    (format "observers/%s.clj" path))))

(defn- load-all-bundled-observers!
  "Load all observers bundled with Bohr."
  []
  (doseq [observer-path [
                         "os"               
                         "self"             
                         "system/uptime"    
                         "system/cpu"       
                         "system/memory"    
                         "system/users"     
                         "system/fs"        
                         "system/disk"      
                         "system/net"       
                         "system/ps"        
                         "system/checksums"
                         ]]
    (load-bundled-observer! observer-path)))
  
(defn load-bundled-observers!
  "Load observers bundled with Bohr according to the given
  `runtime-options`."
  [runtime-options]
  (let [observers
        (get (or (get-config :bohr) {}) :observers)]
    (cond
      (or (nil? observers)
          (= true observers))
      (load-all-bundled-observers!)
      
      (seq? observers)
      (doseq [observer-path observers]
        (println "LOADING" observer-path)
        (load-bundled-observer! observer-path)))))

(defn- load-bundled-journal!
  "Load the script at the given `path` relative to the bundled
  journals directory."
  [path]
  (load-script-file!
   (io/resource
    (format "journals/%s.clj" path))))

(defn load-bundled-journals!
  "Load journals bundled with Bohr according to the given
  `runtime-options`."
  [runtime-options]
  (doseq [journal-path
          (get (or (get-config :bohr) {}) :journals)]
    (load-bundled-journal! journal-path)))
