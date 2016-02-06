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

(defmacro static [name value]
  `(define-observer! ~name {} (fn [] ~value) []))

