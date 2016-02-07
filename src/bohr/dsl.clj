(ns bohr.dsl
  (:require [clojure.tools.logging :as log])
  (:use bohr.notebook)
  (:use bohr.observers)
  (:use bohr.journals))

;;;
;;; Code meant to be called after macroexpansion
;;;

(defn force-get-reading! [name]
  (if (known-reading? name) (get-reading name)
      (do
        (take-reading! name (observe name))
        (get-reading name))))

(defn submit-in-observer! [observer-name name value options]
  (submit-with-observer! (get-observer observer-name) name value options)
  nil)

;;;
;;; Code for macroexpansion
;;;

(defn- extract-observer-arguments [macro-form]
  (let [macro-vec   (apply vector macro-form)
        name        (nth macro-vec 1)
        options-vec (subvec macro-vec 2 (count macro-vec))
        options     (apply hash-map options-vec)]
    (vector name options)))

(defn- replace-observer-references [instructions] instructions)

(defn- calculate-dependencies [instructions] [])

(defn- process-instructions [instructions]
  [(replace-observer-references instructions)
   (calculate-dependencies instructions)])

(defmacro static [name value]
  (let [[name options]
        (extract-observer-arguments (butlast &form))]
    `(let [initial-value# ~(last &form)]
       (define-observer!
         ~name
         ~options
         (fn [] initial-value#)
         []))))

(defmacro measure [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))
        [processed-instructions dependencies]
        (process-instructions (last &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~processed-instructions)
       ~dependencies)))
