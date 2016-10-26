(ns tomato.core
  (:require [tomato.config :as config]
            [tomato.morse :as telegram]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all])
  (:gen-class))


(def counted (atom 1))
(def state (atom {:timer      nil
                  :interval   nil
                  :started    nil
                  :mode       nil
                  :message-id nil}))



(defn secs [s] (* s 1000))
(defn mins [m] (* m (secs 60)))
(def modes {:pomodoro {:text #(str "업무로 돌아올 시간입니다! " @counted), :next :relax, :during (mins 5)}
            :relax    {:text #(str "쉴 시간입니다!"), :next :pomodoro, :during (secs 30)}})




(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn set-timeout [callback ms]
  (future (do (Thread/sleep ms)
              (swap! state assoc :timer nil)
              (callback))))


(defn send-m
  ([message] (telegram/send-text (config/get :token) (config/get :chat-id) message))
  ([message options] (telegram/send-text (config/get :token) (config/get :chat-id) options message)))

(defn edit-m
  ([message id] (telegram/edit-text (config/get :token) (config/get :chat-id) id message))
  ([message id options] (telegram/edit-text (config/get :token) (config/get :chat-id) id options message)))



(defn elapsed-time []
  (- (System/currentTimeMillis) (:started @state)))

(defn base-time []
  (:during ((:mode @state) modes)))

(defn ms->sec [ms]
  (quot ms 1000))

(defn remaining-time []
  (ms->sec (- (base-time) (elapsed-time))))

(defn session-alive? [state]
  (not (nil? (:timer state))))

(defn session-paused? [state]
  (and (nil? (:timer state)) (not (nil? (:elapsed state)))))



(defn send-remaining [message-id byline]
  (if (nil? message-id)
    (->> (send-m (str "남은 시간은 " (remaining-time) "초") {:disable_notification true})
         :result
         :message_id
         (swap! state assoc :message-id))
    (edit-m (str "남은 시간은 " (remaining-time) "/" (ms->sec (base-time)) "초 by " byline) message-id)))

(defn remaining-each-10s [get-messageid]
  (set-interval #(send-remaining (get-messageid) "interval") (secs 10)))



(defn goto-x [key]
  (let [mode (key modes)]
    (when-not (nil? (:interval @state)) (future-cancel (:interval @state)))
    (swap! state assoc
           :mode key
           :started (System/currentTimeMillis)
           :message-id nil
           :interval (remaining-each-10s #(:message-id @state))
           :timer (set-timeout #(goto-x (:next mode)) (:during mode)))

    (when (= key :relax)
      (swap! counted inc))
    (send-m ((:text mode)))))


;;;; Handlers
(defn start-session []
  (if (session-alive? @state)
    (send-m "이미 세션이 진행중입니다")
    (goto-x :pomodoro)))

(defn check-remaining []
  (if (session-alive? @state)
    (send-remaining (:message-id @state) "manual")
    (send-m "세션이 진행중이 아닙니다")))

(defn cancel-session []
  (if (session-alive? @state)
    (do
      (future-cancel (:timer @state))
      (future-cancel (:interval @state))
      (swap! state assoc :timer nil)
      (send-m (str (:mode @state) " 취소되었습니다")))
    (send-m "취소할 세션이 없습니다")))

(defn send-counted [counted]
  (send-m (str counted)))

(defn pause-session []
  (if (session-alive? @state)
    (do
      (future-cancel (:timer @state))
      (future-cancel (:interval @state))
      (swap! state assoc
             :elapsed (elapsed-time)
             :timer nil)
      (send-m (str (:mode @state) " 잠시 중단 되었습니다")))
    (send-m "중단할 세션이 없습니다")))

(defn resume-session []
  (if (session-paused? @state)
    (let [elapsed (:elapsed @state)
          mode ((:mode @state) modes)
          during (- (:during mode) elapsed)]

      (swap! state assoc
             :started (- (System/currentTimeMillis) elapsed)
             :timer (set-timeout #(goto-x (:next mode)) during)
             :interval (remaining-each-10s #(:message-id @state)))
      (send-m "세션을 다시 시작합니다"))
    (send-m "다시 시작할 세션이 없습니다")))

(defhandler bot-api
            (command "go" [] (start-session))
            (command "check" [] (check-remaining))
            (command "count" [] (send-counted @counted))
            (command "cancel" [] (cancel-session))
            (command "pause" [] (pause-session))
            (command "resume" [] (resume-session)))

(def channel (atom nil))

(defn start []
  (reset! channel (p/start (config/get :token) bot-api)))

(defn stop []
  (p/stop @channel))

(defn restart []
  (do (stop)
      (start)))

(defn -main
  "I don't do a whole lot ... yet."
  []
  (println "Hello, World!"))

;(start)
