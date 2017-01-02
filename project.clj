(defproject tomato "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [morse "0.2.2" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [clj-http "3.3.0"]
                 [clj-time "0.12.2"]
                 [amazonica "0.3.77" :exclusions [
                                                  com.amazonaws/aws-java-sdk
                                                  com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.11.26"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.26"]
                 [com.amazonaws/aws-java-sdk-cloudwatch "1.11.26"]
                 [com.amazonaws/aws-java-sdk-events "1.11.26"]
                 [com.amazonaws/aws-lambda-java-core "1.1.0"]]
  :plugins [[lein-cloverage "1.0.8"]]
  :main ^:skip-aot tomato.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
