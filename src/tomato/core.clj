(ns tomato.core
  (:require [tomato.config :as config]
            [tomato.morse :as telegram]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all])
  (:gen-class))

(defn secs [s] (* s 1000))
(defn mins [m] (* m (secs 60)))
(def tasks {:pomodoro {:text "업무로 돌아올 시간입니다!", :next :relax, :during (mins 5)}
            :relax {:text "쉴 시간입니다!", :next :pomodoro, :during (secs 30)}})



; too many atoms
(def timer (atom nil))
(def started (atom nil))
(def tasked (atom nil))


(defn set-timer [callback ms]
  (reset! timer (future (do (Thread/sleep ms)
                            (reset! timer nil)
                            (callback)))))

(defn send-m
  ([message] (telegram/send-text (config/get :token) (config/get :chat-id) message))
  ([message options] (telegram/send-text (config/get :token) (config/get :chat-id) options message)))

(defn goto-x [key]
  (let [task (key tasks)]
    (send-m (:text task))
    (reset! tasked key)
    (reset! started (System/currentTimeMillis))
    (set-timer #(goto-x (:next task)) (:during task))))

(defn remaining-time [key]
  (quot (- (:during (key tasks)) (- (System/currentTimeMillis) @started)) 1000))


;;;; Handlers
(defn timer-start []
  (goto-x :pomodoro))

(defn check-remaining [_]
  (if-not (nil? @timer)
    (send-m (str "남은 시간은 " (remaining-time @tasked) "초"))
    (send-m "타이머가 돌고 있지 않음")))

(defn cancel-timer []
  (if-not (nil? @timer)
    (do
      (future-cancel @timer)
      (reset! timer nil)
      (send-m (str @tasked " 취소되었습니다")))
    (send-m "진행중인 태스크가 없습니다")))

(defhandler bot-api
            (command "go" message (timer-start))
            (command "check" message (check-remaining message))
            (command "cancel" [] (cancel-timer)))

(def channel (atom nil))

(defn start []
  (reset! channel (p/start (config/get :token) bot-api)))

(defn stop []
  (p/stop @channel))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))


(defn send-message [message]
  (send-m {:reply_markup {:keyboard [[{:text "/go"} {:text "/check"}]]
                          :one_time_keyboard true}}
          "시작해볼까요?"))
