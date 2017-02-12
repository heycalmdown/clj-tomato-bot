(ns tomato.handler
  (:require [tomato.config :as config]
            [tomato.core :refer :all]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all]
            [tomato.cloudwatch :as watch]
            [tomato.s3 :as s3]))


(defn handle-start-session! []
  (if (session-alive? (get-state!))
    (send-m! "이미 세션이 진행중입니다")
    (goto-x! :pomodoro)))

(defn handle-check-remaining! [state]
  (if (session-alive? state)
    (send-remaining! state "manual")
    (if (:elapsed state)
      (let [mode ((:mode state) modes)]
        (send-m! (str (:mode state) " 모드가 중단 중입니다 (" (ms->sec (:elapsed state)) "/" (ms->sec (:during mode)) ")")))
      (send-m! "세션이 진행중이 아닙니다"))))

(defn handle-cancel-session! []
  (let [state (get-state!)]
    (if (session-alive? state)
      (do
        (tomato.s3/update! "state" assoc :timer nil)
        (send-m! (str (:mode state) " 취소되었습니다")))
      (send-m! "취소할 세션이 없습니다"))))

(defn handle-send-counted! [counted]
  (send-m! (str counted)))

(defn handle-pause-session! []
  (let [state (get-state!)
        mode  ((:mode state) modes)]
    (if (session-alive? state)
      (do
        (tomato.s3/update! "state"
                           assoc
                           :elapsed (elapsed-time! (:started state))
                           :timer nil)
        (send-m! (str (:mode state) " 잠시 중단 되었습니다 " (ms->sec (elapsed-time! (:started state))) "/" (ms->sec (:during mode)))))
      (send-m! "중단할 세션이 없습니다"))))


(defn handle-resume-session [state]
  (if-not (session-paused? state)
    (send-m! "다시 시작할 세션이 없습니다")
    (let [elapsed (:elapsed state)
          mode    ((:mode state) modes)]
      (send-m! (str "세션을 다시 시작합니다 " (ms->sec elapsed) "/" (ms->sec (:during mode))))
      (tomato.s3/update! "state" assoc
                         :timer true
                         :started (- (now!) elapsed)))))

(defn handle-watch []
  (do (watch/create-watch)
      (println "create-watch!")))

(defn handle-unwatch []
  (do
      (println "cancel-watch!")))

(defn handle-get-state! [state]
  (send-m! (encode state)))

(defhandler bot-api
            (command "go" [] (handle-start-session!))
            (command "check" [] (handle-check-remaining! (get-state!)))
            (command "count" [] (handle-send-counted! (get-counted!)))
            (command "cancel" [] (handle-cancel-session!))
            (command "pause" [] (handle-pause-session!))
            (command "resume" [] (handle-resume-session (get-state!)))
            (command "state" [] (handle-get-state! (get-state!)))
            (command "watch" [] (handle-watch))
            (command "unwatch" [] (handle-unwatch)))

(def channel (atom nil))

(defn start []
  (do (reset! channel (p/start (config/get! :token) bot-api))
      (let [prev-state (tomato.s3/get! "state")]
        (if-let [key (:mode prev-state)]
          (let [started (:started prev-state)
                mode (key modes)
                during (base-time key)
                elapsed (elapsed-time! started)]
            (if (< during elapsed)
              (goto-x! (:next mode))))))))

(defn stop []
  (p/stop @channel))

(defn restart []
  (do (stop)
      (start)))
