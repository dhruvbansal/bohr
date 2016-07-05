(ns bohr.dsl.macros
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:use bohr.observers
        bohr.journals))

(defn- extract-observer-arguments
  "Helper function for extracting the arguments to pass to the
  `define-observer!` function from within the `observe` macro defined
  below."
  [macro-form]
  (let [macro-vec   (apply vector macro-form)
        name        (nth macro-vec 1)
        options-vec (subvec macro-vec 2 (count macro-vec))
        options     (apply hash-map options-vec)]
    (vector name options)))

(defmacro observe
  "Define an observer."
  [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~(last &form)))))
