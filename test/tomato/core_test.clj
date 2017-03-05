(ns tomato.core-test
  (:require [clojure.test :refer :all]
            [tomato.core :refer :all]
            [tomato.config :as config]
            [morse.api :as telegram]
            [tomato.s3 :as s3]
            [tomato.cloudwatch :as cwe]))

(deftest a-test
  (testing "ms->sec"
    (is (= (ms->sec 999) 0))
    (is (= (ms->sec 1000) 1))
    (is (= (ms->sec 1001) 1)))
  (testing "session-alive?"
    (is (= (session-alive? {:timer nil}) false))
    (is (= (session-alive? {:timer true}) true)))
  (testing "session-paused?"
    (is (= (session-paused? {:timer nil :elapsed 1}) true))
    (is (= (session-paused? {:timer true :elapsed 1}) false))
    (is (= (session-paused? {:timer nil}) false))
    (is (= (session-paused? {:timer true}) false))))

(deftest time-test
  (testing "base-time"
    (is (= (base-time :pomodoro) (mins 10)))
    (is (= (base-time :relax) (mins 2))))
  (testing "elapsed"
    (with-redefs-fn {#'now! (fn [] 1000)}
      #(do
         (is (= (now!) 1000))
         (is (= (elapsed-time! 500) 500)))))
  (testing "remaining"
    (with-redefs-fn {#'now! (fn [] (mins 1))}
      #(do
         (is (= (remaining-secs! {:mode :relax :started (mins 1)}) 120))
         (is (= (remaining-secs! {:mode :relax :started (- (mins 1) (secs 25))}) 95090))))))

(deftest sent-message-test
  (testing "message-id"
    (let [message {:ok true,
                   :result {:message_id 1094,
                            :from {:id 1234, :first_name "tomato", :username "k_tomato_bot"},
                            :chat {:id 3456, :first_name "Kei", :last_name "Son",
                                   :username "heycalmdown", :type "private"},
                            :date 1477809856,
                            :text "haha"}}]
      (is (= (pluck-message-id message) 1094))
      (let [test-atom (atom {:message-id nil})]
        (with-redefs-fn {#'send-m!    (fn [_ _] message)
                         #'s3/update! (fn [_ f x y] (swap! test-atom f x y))
                         #'get-state! (fn [] (deref test-atom))}
          #(do
             (time-send! "" 1)
             (is (= (:message-id (get-state!)) 1094))))))))

(deftest lang-test
  (testing "remain"
    (is (= (lang-remaining 10 30 "test") "남은 시간은 10/30초 by test"))))

(deftest stateful-test
  (testing "send-remaining"
    (with-redefs-fn {#'now!       (fn [] 0)
                     #'time-send! (fn [message message-id] {:message message :message-id message-id})
                     #'edit-m!    (fn [message message-id] {:message message :message-id message-id})}
      #(do
         (is (= (send-remaining! {:mode  :relax
                                :started 0}
                                 "test")
                {:message "남은 시간은 30/30초 by test" :message-id nil}))
         (is (= (send-remaining! {:mode     :relax
                                  :started  0
                                :message-id 1}
                                 "test")
                {:message "남은 시간은 30/30초 by test" :message-id 1}))))))

(deftest telegram-test
  (testing "send-m"
    (with-redefs-fn
      {#'telegram/send-text
       (fn
         ([token chat-id message] {:token token :chat-id chat-id :message message})
         ([token chat-id options message] {:token token :chat-id chat-id :options options :message message}))}
      #(let [without-options (send-m! "message")
             with-options (send-m! "message" {:option true})]
        (is (= (:token without-options) (config/get! :token)))
        (is (= (:option (:options with-options)) true)))))
  (testing "edit-m"
    (with-redefs-fn
      {#'telegram/edit-text
       (fn
         ([token chat-id message-id message]
          {:token token :chat-id chat-id :message-id message-id :message message})
         ([token chat-id message-id options message]
          {:token token :chat-id chat-id :options options :message-id message-id :message message}))}
      #(let [without-options (edit-m! "message" 1)
             with-options (edit-m! "message" 1 {:option true})]
        (is (= (:message-id without-options) 1))))))