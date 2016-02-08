(ns bohr.dsl
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string])
  (:use bohr.notebook
        bohr.observers
        bohr.journals
        bohr.dependencies))

(defn & [name]
  (register-dependency! current-observer name)
  (if (known-reading? name) (get-reading name)
      (do
        (take-reading! name (make-observation name))
        (get-reading name))))

(defn- extract-observer-arguments [macro-form]
  (let [macro-vec   (apply vector macro-form)
        name        (nth macro-vec 1)
        options-vec (subvec macro-vec 2 (count macro-vec))
        options     (apply hash-map options-vec)]
    (vector name options)))

(defmacro static [name value]
  (let [[name options]
        (extract-observer-arguments (butlast &form))]
    `(let [initial-value# ~(last &form)]
       (define-observer!
         ~name
         ~options
         (fn [] initial-value#)))))

(defmacro observe [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~(last &form)))))
