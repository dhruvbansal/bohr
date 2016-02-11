(defproject bohr "0.1.0"
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
                 [riemann-clojure-client "0.4.2"]
                 [clj-yaml "0.4.0"]
                 [clj-yaml "0.4.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.15"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 ]
  :main ^:skip-aot bohr.core
  :target-path "target"
  
  :plugins [[lein-bin "0.3.4"]]
  :bin {:name "bohr"}


  :profiles {:uberjar {:aot :all}})
