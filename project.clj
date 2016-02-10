(defproject bohr "0.1.0-SNAPSHOT"
  :description "Gathers metrics from your systems"
  :url "http://github.com/dhruvbansal/bohr"
  :license {:name "Apache Public License (2.0)"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [
                 [org.clojure/clojure       "1.7.0"  ]
                 [clj-time                  "0.11.0" ]
                 [overtone/at-at            "1.2.0"  ]
                 [org.clojure/tools.cli     "0.3.3"  ]
                 [org.clojure/tools.logging "0.3.1"  ]
                 [clj-logging-config        "1.9.3"  ]
                 [table "0.5.0"]
                 ]
  :main ^:skip-aot bohr.core
  :target-path "target/%s"

  :repl-options {
                 ;; This expression will run when first opening a REPL, in the
                 ;; namespace from :init-ns or :main if specified.
                 :init (do (in-ns bohr.repl) (refer-clojure))
                 ;; Skip's the default requires and printed help message.
                 :skip-default-init false
                 }

  :profiles {:uberjar {:aot :all}})
