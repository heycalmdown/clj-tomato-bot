(ns tomato.handler
  (:require [tomato.config :as config]
            [tomato.core :refer :all]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all]))


(defn handle-start-session []
  (if (session-alive? (get-state))
    (send-m "이미 세션이 진행중입니다")
    (goto-x :pomodoro)))

(defn handle-check-remaining [state]
  (if (session-alive? state)
    (send-remaining state "manual")
    (send-m "세션이 진행중이 아닙니다")))

(defn handle-cancel-session []
  (let [state (get-state)]
    (if (session-alive? state)
      (do
        (future-cancel (:timer state))
        (future-cancel (:interval state))
        (swap! state-atom assoc :timer nil)
        (send-m (str (:mode state) " 취소되었습니다")))
      (send-m "취소할 세션이 없습니다"))))

(defn handle-send-counted [counted]
  (send-m (str counted)))

(defn handle-pause-session []
  (let [state (get-state)]
    (if (session-alive? state)
      (do
        (future-cancel (:timer state))
        (future-cancel (:interval state))
        (swap! state-atom assoc
               :elapsed (elapsed-time (:started state))
               :timer nil)
        (send-m (str (:mode state) " 잠시 중단 되었습니다")))
      (send-m "중단할 세션이 없습니다"))))


(defn resume [mode elapsed]
  (let [remained (- (:during mode) elapsed)]

    (swap! state-atom assoc
           :timer (set-timeout #(goto-x (:next mode)) remained)
           :interval (remaining-each-10s #(get-state)))
    (send-m "세션을 다시 시작합니다")))


(defn handle-resume-session [state]
  (if-not (session-paused? state)
    (send-m "다시 시작할 세션이 없습니다")
    (let [elapsed (:elapsed state)
          mode ((:mode state) modes)]
      (resume mode elapsed)
      (tomato.s3/reset! "state" {:mode    (:mode state)
                                 :started (- (current-time) elapsed)}))))

(defhandler bot-api
            (command "go" [] (handle-start-session))
            (command "check" [] (handle-check-remaining (get-state)))
            (command "count" [] (handle-send-counted @counted))
            (command "cancel" [] (handle-cancel-session))
            (command "pause" [] (handle-pause-session))
            (command "resume" [] (handle-resume-session (get-state))))

(def channel (atom nil))

(defn start []
  (do (reset! channel (p/start (config/get :token) bot-api))
      (let [prev-state (tomato.s3/read "state")]
        (if-let [key (:mode prev-state)]
          (let [started (:started prev-state)
                mode (key modes)
                during (base-time key)
                elapsed (elapsed-time started)]
            (if (< during elapsed)
              (goto-x (:next mode))
              (resume mode elapsed)))))))

(defn stop []
  (p/stop @channel))

(defn restart []
  (do (stop)
      (start)))
