;;;; Defines the core functions which enable the Bohr DSL.

(ns bohr.dsl
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:use bohr.notebook
        bohr.observers
        bohr.journals))

(defn- extract-observer-arguments
  "Helper function for extracting the arguments to pass to the
  `define-observer!` function from within the `static` and `observe`
  macros defined below."
  [macro-form]
  (let [macro-vec   (apply vector macro-form)
        name        (nth macro-vec 1)
        options-vec (subvec macro-vec 2 (count macro-vec))
        options     (apply hash-map options-vec)]
    (vector name options)))

(defmacro static
  "Define an observer whose reading never expires.

  A 'static' observer will make its reading when the script is first
  loaded, prior to Bohr requesting more general observers to make
  their first readings."
  [name value]
  (let [[name options]
        (extract-observer-arguments (butlast &form))]
    `(let [initial-value# ~(last &form)]
       (define-observer!
         ~name
         ~options
         (fn [] initial-value#)))))

(defmacro observe
  "Define an observer."
  [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~(last &form)))))
