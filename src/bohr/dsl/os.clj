;;; os.clj
;;;
;;; Defines functions that interrogate the operating system.  These
;;; are to be relied upon when defining observers which need to
;;; interact with the operating system, typically via case statements:
;;;
;;;   (case-os
;;;     "Linux"  (...)
;;;     "Darwin" (...)
;;;     ...

(ns bohr.dsl.os
  (:require [clojure.tools.logging :as log])
  (:use bohr.observers
        bohr.dsl.helpers
        bohr.dsl.parsers))

(def kernel-name      (sh-output "uname -s"))
(def kernel-release   (sh-output "uname -r"))
(def kernel-version   (sh-output "uname -v"))
(def kernel-processor (sh-output "uname -p"))

(case kernel-name
  "Linux"
  (let [os-info
        (parse-properties (sh-output "lsb_release -a"))]
    (def os-type     "Linux")
    (def os-name     (get os-info "Distributor ID" "GNU/Linux"))
    (def os-release  (get os-info "Release"        "unknown"))
    (def os-version  (get os-info "Description"    "unknown"))
    (def os-codename (get os-info "Codename"))
    (def os-family
      (case (get os-info "Distributor ID" "GNU/Linux")
        ("Ubuntu" "Debian") "Debian"
        ;; FIXME -- add RedHet CentOS &c.
        (get os-info "Distributor ID" "GNU/Linux"))))

  "Darwin"
  (let [os-info
        (parse-properties (sh-output "sw_vers"))]
    (def os-type    "Mac")
    (def os-name    (get os-info "ProductName"    "Mac OS X"))
    (def os-release (get os-info "ProductVersion" "unknown"))
    (def os-version (get os-info "BuildVersion"   "unknown"))
    (def os-family  "Mac"))

  (log/error "Unknown kernel:" kernel-name))

(defmacro case-os [& clauses]
  `(case os-type
     ~@clauses
     (log/error "Cannot observe" current-observer "for OS" os-type)))
