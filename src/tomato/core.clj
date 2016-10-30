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

(defn current-time []
  (System/currentTimeMillis))


(defn elapsed-time [started]
  (- (current-time) started))

(defn base-time [mode]
  (:during (mode modes)))

(defn ms->sec [ms]
  (quot ms 1000))

(defn remaining-secs [state]
  (ms->sec (- (base-time (:mode state)) (elapsed-time (:started state)))))

(defn session-alive? [state]
  (not (nil? (:timer state))))

(defn session-paused? [state]
  (and (nil? (:timer state)) (not (nil? (:elapsed state)))))

(defn pluck-message-id [sent]
  (:message_id (:result sent)))

(defn lang-remaining [remaining base byline]
  (str "남은 시간은 " remaining "/" base "초 by " byline))


(defn time-send [message _]
  (let [sent (send-m message {:disable_notification true})]
    (swap! state-atom assoc :message-id (pluck-message-id sent))))


(defn send-remaining [state byline]
  (let [remaining (remaining-secs state)
        base (ms->sec (base-time (:mode state)))
        message (lang-remaining remaining base byline)
        message-id (:message-id state)
        action (cond (nil? message-id) time-send
                     :else edit-m)]
    (apply action [message message-id])))

(defn remaining-each-10s [get-state]
  (set-interval #(send-remaining (get-state) "interval") (secs 10)))



(defn goto-x [key]
  (let [mode (key modes)
        interval (:interval @state-atom)]
    (when-not (nil? interval) (future-cancel interval))
    (swap! state-atom assoc
           :mode key
           :started (current-time)
           :message-id nil
           :interval (remaining-each-10s #(deref state-atom))
           :timer (set-timeout #(goto-x (:next mode)) (:during mode)))

    (when (= key :relax)
      (swap! counted inc))
    (send-m ((:text mode)))))


;;;; Handlers

(defn -main
  "I don't do a whole lot ... yet."
  []
  (println "Hello, World!"))

