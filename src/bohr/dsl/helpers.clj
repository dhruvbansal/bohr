;;;; Contains helper functions intended to be used from within the
;;;; Bohr DSL.

(ns bohr.dsl.helpers
  (:require [clojure.string  :as string]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clj-http.client :as httpclient])
  (:use clojure.java.shell
        bohr.observers
        bohr.journals))

(defn sh-output
  "Returns the (newline-trimmed) output of a shell `command` or
  pipeline."
  [command]
  (string/trim (get (sh "bash" "-c" command) :out)))

;; The directory containing the /proc filesystem.
;;
;; Should only be accessed on Linux systems which implement the /proc
;; filesystem, but harmless to define it here.
(def procfile-dir (io/file "/proc"))

(defn procfile-contents
  "Return the (newline-trimmed) contents of the procfile at the given
  path, relative to the `procfile-dir`.

  Given the `procfile-dir` is /proc, the procfile /proc/stat can be
  referred to via the path `stat`."
  [path]
  (string/trim
   (slurp
    (java.io.FileReader.
     (io/file procfile-dir path)))))

(defn sysctl
  "Return the value of the sysctl variable `name`."
  [name]
  (sh-output (format "sysctl -n '%s'" name)))

(defn translate [source mapping]
  (into
   {}
   (map
    (fn [[source_name target_name]] [target_name (get source source_name)])
    (seq mapping))))

(defn annotate [values annotations]
  (into
   {}
   (map
    (fn [[key value]]
      (let [annotation (get annotations key)]
        [key {:value value
              :desc  (get annotation :desc)
              :units (get annotation :units)
              :attrs (get annotation :attrs)
              }]))
    (seq values))))

(defn http-get
  "Returns the result of an HTTP GET"
  [url]
  (get (httpclient/get url) :body))

(defn http-get-json
  "Returns the result of an HTTP GET as JSON"
  [url]
  (get (httpclient/get url {:as :json}) :body ))

(defn http-post
  "Returns the result of an HTTP POST"
  [url jsonbody]
  (get (httpclient/post url {:form-params jsonbody :content-type :json}) :body))

(defn http-post-json
  "Returns the result of an HTTP POST as JSON"
  [url jsonbody]
  (get (httpclient/post url {:form-params jsonbody :content-type :json :as :json}) :body ))
