;;; os.clj
;;;
;;; Defines operating system observers.  These are to be relied upon
;;; when defining other observers which need to interact with the
;;; operating system, typically via case statements:
;;;
;;;   (case (:kernel.name)
;;;     "Linux"  (...)
;;;     "Darwin" (...)
;;;     ...

(let [kernel-name (sh-output "uname" "-s")]
  (static :kernel.name      kernel-name)
  (static :kernel.release   (sh-output "uname" "-r"))
  (static :kernel.version   (sh-output "uname" "-v"))
  (static :kernel.processor (sh-output "uname" "-p"))

  (case kernel-name
    
    "Linux"
    (let [os-info
          (parse-two-columns (sh-output "lsb_release" "-a"))]
      (println "running linux")
      (static :os.name     (get os-info "Distributor ID" "GNU/Linux"))
      (static :os.release  (get os-info "Release"        "unknown"))
      (static :os.version  (get os-info "Description"    "unknown"))
      (static :os.codename (get os-info "Codename")))

    "Darwin"
    (let [os-info
          (parse-two-columns (sh-output "sw_vers"))]
      (println "running darwin")
      (static :os.name    (get os-info "ProductName"    "Mac OS X"))
      (static :os.release (get os-info "ProductVersion" "unknown"))
      (static :os.version (get os-info "BuildVersion"   "unknown")))
    
    (log/error "Unknown kernel:" kernel-name)))
