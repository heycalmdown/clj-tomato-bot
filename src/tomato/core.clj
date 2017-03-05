(ns tomato.core
  (:require [tomato.config :as config]
            [morse.api :as telegram]
            [cheshire.core :refer :all]
            [tomato.s3 :as s3]
            [tomato.cloudwatch :as cwe]
            [clj-time.local :as l]
            [clj-time.format :as f])
  (:gen-class))


(defn get-state! []
  (s3/get! "state"))


(defn today! []
  (f/unparse (f/formatter :year-month-day) (l/local-now)))

(defn get-today! [date]
  (let [today (s3/get! "today")]
    (if-not (= date (:date today))
      {:date date :counted 0}
      today)))

(defn get-counted! []
  (:counted (get-today! (today!))))

(defn inc-counted! []
  (let [today (get-today! (today!))]
    (s3/put! "today" {:date    (:date today)
                      :counted (inc (:counted today))})))



(defn secs [s] (* s 1000))
(defn mins [m] (* m (secs 60)))
(def modes {:pomodoro {:text #(str "업무로 돌아올 시간입니다! " (get-counted!)), :next :relax, :during (mins 10)}
            :relax    {:text #(str "쉴 시간입니다!"), :next :pomodoro, :during (mins 2)}})


(defn send-m!
  ([message] (telegram/send-text (config/get! :token) (config/get! :chat-id) message))
  ([message options] (telegram/send-text (config/get! :token) (config/get! :chat-id) options message)))

(defn edit-m!
  ([message id] (telegram/edit-text (config/get! :token) (config/get! :chat-id) id message))
  ([message id options] (telegram/edit-text (config/get! :token) (config/get! :chat-id) id options message)))

(defn now! []
  (System/currentTimeMillis))


(defn elapsed-time! [started]
  (- (now!) started))

(defn base-time [mode]
  (:during (mode modes)))

(defn ms->sec [ms]
  (quot ms 1000))

(defn remaining-secs! [state]
  (ms->sec (- (base-time (:mode state)) (elapsed-time! (:started state)))))

(defn session-alive? [state]
  (not (nil? (:timer state))))

(defn session-paused? [state]
  (and (nil? (:timer state)) (not (nil? (:elapsed state)))))

(defn pluck-message-id [sent]
  (:message_id (:result sent)))

(defn lang-remaining [remaining base byline]
  (str "남은 시간은 " remaining "/" base "초 by " byline))


(defn time-send! [message _]
  (let [sent (send-m! message {:disable_notification true})]
    (s3/update! "state" assoc :message-id (pluck-message-id sent))))


(defn send-remaining! [state byline]
  (let [remaining (remaining-secs! state)
        base (ms->sec (base-time (:mode state)))
        message (lang-remaining remaining base byline)
        message-id (:message-id state)
        action (cond (nil? message-id) time-send!
                     :else edit-m!)]
    (apply action [message message-id])))

(defn goto-x! [key]
  (let [mode (key modes)]
    (cwe/ensure-timer!)
    (s3/put! "state" {:mode       key
                      :started    (now!)
                      :message-id nil
                      :timer      true})

    (when (= key :relax)
      (inc-counted!))
    (send-m! ((:text mode)))))


;;;; Handlers

(defn -main
  "I don't do a whole lot ... yet."
  []
  (println "Hello, World!"))

