(ns tomato.core
  (:require [tomato.config :as config]
            [tomato.morse :as telegram]
            [cheshire.core :refer :all]
            [morse.polling :as p]
            [morse.handlers :refer :all])
  (:gen-class))


; [TODO] too many atoms
(def timer (atom nil))
(def interval (atom nil))
(def started (atom nil))
(def in-state (atom nil))
(def counted (atom 1))
(def message-id (atom nil))




(defn secs [s] (* s 1000))
(defn mins [m] (* m (secs 60)))
(def states {:pomodoro {:text #(str "업무로 돌아올 시간입니다! " @counted), :next :relax, :during (mins 5)}
             :relax    {:text #(str "쉴 시간입니다!"), :next :pomodoro, :during (secs 30)}})




(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defn set-timeout [callback ms]
  (reset! timer (future (do (Thread/sleep ms)
                            (reset! timer nil)
                            (callback)))))

(defn send-m
  ([message] (telegram/send-text (config/get :token) (config/get :chat-id) message))
  ([message options] (telegram/send-text (config/get :token) (config/get :chat-id) options message)))

(defn edit-m
  ([message id] (telegram/edit-text (config/get :token) (config/get :chat-id) id message))
  ([message id options] (telegram/edit-text (config/get :token) (config/get :chat-id) id options message)))



(defn elapsed-time []
  (- (System/currentTimeMillis) @started))

(defn base-time []
  (:during (@in-state states)))

(defn ms->sec [ms]
  (quot ms 1000))

(defn remaining-time []
  (ms->sec (- (base-time) (elapsed-time))))

(defn edit-message [id message]
  ;(send-m message)
  (edit-m message id)
  )


(defn send-remaining []
  (if (nil? @message-id)
    (->> (send-m (str "남은 시간은 " (remaining-time) "초"))
         :result
         :message_id
         (reset! message-id))
    (edit-message @message-id (str "남은 시간은 " (remaining-time) "/" (ms->sec (base-time)) "초"))))



(defn goto-x [key]
  (let [state (key states)]
    (send-m ((:text state)))
    (reset! in-state key)
    (reset! started (System/currentTimeMillis))
    (reset! message-id nil)
    (when-not (nil? @interval) (future-cancel @interval))
    (reset! interval (set-interval #(send-remaining) (secs 10)))
    (when (= key :relax)
      (swap! counted inc))
    (set-timeout #(goto-x (:next state)) (:during state))))


;;;; Handlers
(defn timer-start []
  (if (nil? @timer)
    (goto-x :pomodoro)
    (send-m "이미 세션이 진행중입니다")))

(defn check-remaining []
  (if-not (nil? @timer)
    (send-remaining)
    (send-m "타이머가 돌고 있지 않음")))

(defn cancel-timer []
  (if-not (nil? @timer)
    (do
      (future-cancel @timer)
      (future-cancel @interval)
      (reset! timer nil)
      (send-m (str @in-state " 취소되었습니다")))
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
