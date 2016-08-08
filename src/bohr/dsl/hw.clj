;;; hw.clj
;;;
;;; Defines functions for interrogating hardware.  These are to be relied upon
;;; when defining observers which can only access input defined in
;;; terms of, say cpu clock ticks.

(ns bohr.dsl.hw
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:use bohr.dsl.helpers
        bohr.dsl.parsers
        bohr.dsl.os))

(def cpu-clock-tick
  (Float/parseFloat
   (sh-output "getconf CLK_TCK")))

(def page-size
  (Integer/parseInt
   (sh-output "getconf PAGE_SIZE")))

(defn- cpu-count-linux []
  (count
   (filter
    #(re-find #"^processor" %)
    (string/split-lines (procfile-contents "cpuinfo")))))

(defn- cpu-count-mac []
  (Integer/parseInt (sysctl "hw.ncpu")))

(def cpu-count
  (case-os
   "Linux" (cpu-count-linux)
   "Mac"   (cpu-count-mac)))

(defn cpu-ticks-to-time
  ([ticks cpus]
   (/ ticks
      (* cpus cpu-clock-tick)))
  ([ticks]
   (cpu-ticks-to-time ticks cpu-count)))
