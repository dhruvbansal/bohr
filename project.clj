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
                 ]
  :main ^:skip-aot bohr.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
