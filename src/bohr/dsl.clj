(ns bohr.dsl
  (:require [clojure.tools.logging :as log]
            [clojure.walk :as walk])
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

(defn submit-in-observer! [observer-name name value & options]
  (submit-with-observer! (get-observer observer-name) name value (apply hash-map options))
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

(defn- observer-reference? [form]
  (and
   (list? form)
   (= 1 (count form))
   (keyword? (first form))))

(defn- replace-observer-references [form]
  (walk/postwalk
   #(if (observer-reference? %)
      (list 'force-get-reading! (first %))
      %)
   form))

(defn- submission? [form]
  (and
   (list? form)
   (= 'submit (first form))))

(defn- contextualize-submissions [name form]
  (walk/postwalk
   #(if (submission? %)
      (conj (rest %) name 'submit-in-observer!)
      %)
   form))

(defn- calculate-dependencies [form]
  (let [dependencies (atom [])]
    (walk/postwalk
     #(do
        (if (observer-reference? %)
          (swap! dependencies conj (first %)))
        %)
     form)
    @dependencies))
  
(defn- process-instructions [name instructions]
  [(contextualize-submissions name
    (replace-observer-references instructions))
   (calculate-dependencies      instructions)])

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
        (process-instructions name (last &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~processed-instructions)
       ~dependencies)))

(defmacro calc [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))
        [processed-instructions dependencies]
        (process-instructions name (last &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~processed-instructions)
       ~dependencies)))

(defmacro calculate [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))
        [processed-instructions dependencies]
        (process-instructions name (last &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~processed-instructions)
       ~dependencies)))

(defmacro report [& args]
  (let [[name options]
        (extract-observer-arguments (butlast &form))
        [processed-instructions dependencies]
        (process-instructions name (last &form))]
    `(define-observer!
       ~name
       ~options
       (fn [] ~processed-instructions)
       ~dependencies)))
