(ns tomato.cloudwatch
  (:require [amazonica.aws.cloudwatchevents :as cwe]
            [tomato.config :as config]
            [cheshire.core :as json]))

(defn create-watch []
  (println (cwe/put-rule
             (config/get :aws)
             :name "test"
             :description "test 1min"
             :schedule-expression "rate(1 minute)"))
  (println
    (cwe/put-targets
      (config/get :aws)
      :rule "test"
      :targets [{:id    "tick"
                 :arn   "arn:aws:lambda:ap-northeast-2:472696305832:function:handleTick"}])))

(defn cancel-watch []
  (try
    (do
      (println (cwe/remove-targets (config/get :aws) :rule "test" :ids ["tick"]))
      (println (cwe/delete-rule
                 (config/get :aws)
                 :name "test")))
    (catch com.amazonaws.services.cloudwatchevents.model.ResourceNotFoundException _ ())))

(defn schedule-expression [mins]
  (if (= mins 1)
    "rate(1 minute)"
    (str "rate(" mins " minutes)")))

(defn timeout! [rule mins]
  (println (cwe/put-rule
             (config/get :aws)
             :name rule
             :description (str "test" rule)
             :schedule-expression (schedule-expression mins))))

(defn cancel-timeout! [rule]
  (println (cwe/delete-rule (config/get :aws) :name rule)))
