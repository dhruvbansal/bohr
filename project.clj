(defproject bohr "0.1.0-SNAPSHOT"
  :description "Gathers metrics from your systems"
  :url "http://github.com/dhruvbansal/bohr"
  :license {:name "Apache Public License (2.0)"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :main ^:skip-aot bohr.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
