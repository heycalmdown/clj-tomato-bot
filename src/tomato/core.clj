(ns tomato.core
  (:require [tomato.config :as config]
            [tomato.morse :as telegram]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all])
  (:gen-class))


(def counted (atom 1))
(def state-atom (atom {:timer      nil
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
              (swap! state-atom assoc :timer nil)
              (callback))))


(defn send-m
  ([message] (telegram/send-text (config/get :token) (config/get :chat-id) message))
  ([message options] (telegram/send-text (config/get :token) (config/get :chat-id) options message)))

(defn edit-m
  ([message id] (telegram/edit-text (config/get :token) (config/get :chat-id) id message))
  ([message id options] (telegram/edit-text (config/get :token) (config/get :chat-id) id options message)))



(defn elapsed-time []
  (- (System/currentTimeMillis) (:started @state-atom)))

(defn base-time []
  (:during ((:mode @state-atom) modes)))

(defn ms->sec [ms]
  (quot ms 1000))

(defn remaining-time []
  (ms->sec (- (base-time) (elapsed-time))))

(defn session-alive? [state]
  (not (nil? (:timer state))))

(defn session-paused? [state]
  (and (nil? (:timer state)) (not (nil? (:elapsed state)))))

(defn pluck-message-id [sent-message]
  (:message_id (:result sent-message)))

(defn lang-remaing [remaining-time base-time byline]
  (str "남은 시간은 " remaining-time "/" base-time "초 by " byline))


(defn time-send [message _]
  (let [sent (send-m message {:disable_notification true})]
    (swap! state-atom assoc :message-id (pluck-message-id sent))))

(defn time-edit [message message-id]
  (edit-m message message-id))


(defn send-remaining [state byline]
  (let [remaining (remaining-time)
        base (ms->sec (base-time))
        message (lang-remaing remaining base byline)
        message-id (:message-id state)
        action (cond (nil? message-id) time-send
                     :else time-edit)]
    (apply action [message message-id])))

(defn remaining-each-10s [get-state]
  (set-interval #(send-remaining (get-state) "interval") (secs 10)))



(defn goto-x [key]
  (let [mode (key modes)
        interval (:interval @state-atom)]
    (when-not (nil? interval) (future-cancel interval))
    (swap! state-atom assoc
           :mode key
           :started (System/currentTimeMillis)
           :message-id nil
           :interval (remaining-each-10s #(deref state-atom))
           :timer (set-timeout #(goto-x (:next mode)) (:during mode)))

    (when (= key :relax)
      (swap! counted inc))
    (send-m ((:text mode)))))


;;;; Handlers
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
             :elapsed (elapsed-time)
             :timer nil)
      (send-m (str (:mode @state-atom) " 잠시 중단 되었습니다")))
    (send-m "중단할 세션이 없습니다")))

(defn handle-resume-session []
  (if (session-paused? @state-atom)
    (let [elapsed (:elapsed @state-atom)
          mode ((:mode @state-atom) modes)
          during (- (:during mode) elapsed)]

      (swap! state-atom assoc
             :started (- (System/currentTimeMillis) elapsed)
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

(defn -main
  "I don't do a whole lot ... yet."
  []
  (println "Hello, World!"))

;(start)
