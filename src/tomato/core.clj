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
  (swap! state assoc :timer (future (do (Thread/sleep ms)
                                        (swap! state assoc :timer nil)
                                        (callback)))))


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

(defn edit-message [id message]
  ;(send-m message)
  (edit-m message id)
  )

(defn session-alive? []
  (nil? (:timer @state)))



(defn send-remaining [byline]
  (if (nil? (:message-id @state))
    (->> (send-m (str "남은 시간은 " (remaining-time) "초"))
         :result
         :message_id
         (swap! state assoc :message-id))
    (edit-message (:message-id @state) (str "남은 시간은 " (remaining-time) "/" (ms->sec (base-time)) "초 by " byline))))



(defn goto-x [key]
  (let [mode (key modes)]
    (send-m ((:text mode)))
    (swap! state assoc :mode key)
    (swap! state assoc :started (System/currentTimeMillis))
    (swap! state assoc :message-id nil)
    (when-not (nil? (:interval @state)) (future-cancel (:interval @state)))
    (swap! state assoc :interval (set-interval #(send-remaining "interval") (secs 10)))
    (when (= key :relax)
      (swap! counted inc))
    (set-timeout #(goto-x (:next mode)) (:during mode))))


;;;; Handlers
(defn timer-start []
  (if (session-alive?)
    (goto-x :pomodoro)
    (send-m "이미 세션이 진행중입니다")))

(defn check-remaining []
  (if-not (session-alive?)
    (send-remaining "manual")
    (send-m "타이머가 돌고 있지 않음")))

(defn cancel-timer []
  (if-not (session-alive?)
    (do
      (future-cancel (:timer @state))
      (future-cancel (:interval @state))
      (swap! state assoc :timer nil)
      (send-m (str (:mode @state) " 취소되었습니다")))
    (send-m "진행중인 태스크가 없습니다")))

(defn count-counted []
  (send-m (str @counted)))

(defhandler bot-api
            (command "go" [] (timer-start))
            (command "check" [] (check-remaining))
            (command "count" [] (count-counted))
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

(start)
