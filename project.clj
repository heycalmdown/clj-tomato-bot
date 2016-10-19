(defproject tomato "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [morse "0.2.1"]
                 [clj-http "2.1.0"]]
  :main ^:skip-aot tomato.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
