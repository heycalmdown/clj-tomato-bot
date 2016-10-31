(ns tomato.handler
  (:require [tomato.config :as config]
            [tomato.core :refer :all]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all]))


(defn handle-start-session []
  (if (session-alive? @state-atom)
    (send-m "이미 세션이 진행중입니다")
    (goto-x :pomodoro)))

(defn handle-check-remaining [state]
  (if (session-alive? state)
    (send-remaining state "manual")
    (send-m "세션이 진행중이 아닙니다")))

(defn handle-cancel-session []
  (if (session-alive? @state-atom)
    (do
      (future-cancel (:timer @state-atom))
      (future-cancel (:interval @state-atom))
      (swap! state-atom assoc :timer nil)
      (send-m (str (:mode @state-atom) " 취소되었습니다")))
    (send-m "취소할 세션이 없습니다")))

(defn handle-send-counted [counted]
  (send-m (str counted)))

(defn handle-pause-session []
  (if (session-alive? @state-atom)
    (do
      (future-cancel (:timer @state-atom))
      (future-cancel (:interval @state-atom))
      (swap! state-atom assoc
             :elapsed (elapsed-time (:started @state-atom))
             :timer nil)
      (send-m (str (:mode @state-atom) " 잠시 중단 되었습니다")))
    (send-m "중단할 세션이 없습니다")))

(defn handle-resume-session []
  (if (session-paused? @state-atom)
    (let [elapsed (:elapsed @state-atom)
          mode ((:mode @state-atom) modes)
          during (- (:during mode) elapsed)]

      (swap! state-atom assoc
             :started (- (current-time) elapsed)
             :timer (set-timeout #(goto-x (:next mode)) during)
             :interval (remaining-each-10s #(:message-id @state-atom)))
      (send-m "세션을 다시 시작합니다"))
    (send-m "다시 시작할 세션이 없습니다")))

(defhandler bot-api
            (command "go" [] (handle-start-session))
            (command "check" [] (handle-check-remaining @state-atom))
            (command "count" [] (handle-send-counted @counted))
            (command "cancel" [] (handle-cancel-session))
            (command "pause" [] (handle-pause-session))
            (command "resume" [] (handle-resume-session)))

(def channel (atom nil))

(defn start []
  (reset! channel (p/start (config/get :token) bot-api)))

(defn stop []
  (p/stop @channel))

(defn restart []
  (do (stop)
      (start)))
