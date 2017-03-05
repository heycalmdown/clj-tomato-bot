(defproject tomato "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [morse "0.2.4" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [clj-http "3.4.1"]
                 [clj-time "0.13.0"]
                 [amazonica "0.3.88" :exclusions [
                                                  com.amazonaws/aws-java-sdk
                                                  com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.98"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.98"]
                 [com.amazonaws/aws-java-sdk-cloudwatch "1.11.98"]
                 [com.amazonaws/aws-java-sdk-events "1.11.98"]
                 [com.amazonaws/aws-lambda-java-core "1.1.0"]]
  :plugins [[lein-cloverage "1.0.9"]]
  :main ^:skip-aot tomato.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
