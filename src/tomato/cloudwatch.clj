(ns tomato.cloudwatch
  (:require [amazonica.aws.cloudwatchevents :as cwe]
            [tomato.config :as config]
            [cheshire.core :as json]))

(defn create-watch []
  (println (cwe/put-rule
             (config/get! :aws)
             :name "test"
             :description "test 1min"
             :schedule-expression "rate(1 minute)"))
  (println
    (cwe/put-targets
      (config/get! :aws)
      :rule "test"
      :targets [{:id    "tick"
                 :arn   "arn:aws:lambda:ap-northeast-2:472696305832:function:handleTick"}])))

(defn schedule-expression [mins]
  (if (= mins 1)
    "rate(1 minute)"
    (str "rate(" mins " minutes)")))

(defn ensure-timer! []
  (let [rule-name "ticker"]
    (cwe/put-rule
      (config/get! :aws)
      :name rule-name
      :description "1 min ticker"
      :schedule-expression (schedule-expression 1))
    (cwe/put-targets
      (config/get! :aws)
      :rule rule-name
      :targets [{:id    "tick"
                 :arn   "arn:aws:lambda:ap-northeast-2:472696305832:function:handleTick"
                 :input (json/encode {:type "ticker"})}])))

(defn delete-timer! []
  (do
    (cwe/remove-targets (config/get! :aws) :rule "ticker" :ids ["tick"])
    (cwe/delete-rule (config/get! :aws) :name "ticker")))
