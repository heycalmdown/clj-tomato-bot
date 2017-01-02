(ns tomato.cloudwatch
  (:require [amazonica.aws.cloudwatchevents :as cwe]
            [tomato.config :as config]
            [cheshire.core :as json]))

(defn create-watch []
  (println (cwe/put-rule
             (config/get :cloudwatch)
             :name "test"
             :description "test 1min"
             :schedule-expression "rate(1 minute)"))
  (println
    (cwe/put-targets
      (config/get :cloudwatch)
      :rule "test"
      :targets [{:id    "tick"
                 :arn   "arn:aws:lambda:ap-northeast-2:472696305832:function:handleTick"
                 ;:input (json/generate-string {"whatever" "arguments"})
                 }])))
