(ns tomato.core-test
  (:require [clojure.test :refer :all]
            [tomato.core :refer :all]
            [tomato.config :as config]
            [morse.api :as telegram]))

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
    (is (= (base-time :pomodoro) (mins 5)))
    (is (= (base-time :relax) (secs 30))))
  (testing "elapsed"
    (with-redefs-fn {#'current-time! (fn [] 1000)}
      #(do
         (is (= (current-time!) 1000))
         (is (= (elapsed-time! 500) 500)))))
  (testing "remaining"
    (with-redefs-fn {#'current-time! (fn [] (mins 1))}
      #(do
         (is (= (remaining-secs! {:mode :relax :started (mins 1)}) 30))
         (is (= (remaining-secs! {:mode :relax :started (- (mins 1) (secs 25))}) 5))))))

(deftest delay-test
  (testing "set-timeout"
    (with-redefs [state-atom (atom {:timer      nil
                                    :interval   nil
                                    :started    nil
                                    :mode       nil
                                    :message-id nil})]
      (let [started (current-time!)]
        @(set-timeout! #(is (>= (- (current-time!) started) 100)) 100))))
  (testing "set-interval"
    (with-redefs [state-atom (atom {:timer      nil
                                    :interval   nil
                                    :started    nil
                                    :mode       nil
                                    :message-id nil})]
      (let [test-atom (atom 1)
            interval (set-interval! #(swap! test-atom inc) 100)]

        @(set-timeout! #(do
                        (future-cancel interval)
                        (is (>= @test-atom 3)))
                       300)))))

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
      (with-redefs [state-atom (atom {:timer      nil
                                      :interval   nil
                                      :started    nil
                                      :mode       nil
                                      :message-id nil})]
        (with-redefs-fn {#'send-m! (fn [_ _] message)}
          #(do
             (time-send! "" 1)
             (is (= (:message-id (get-state!)) 1094))))))))

(deftest lang-test
  (testing "remain"
    (is (= (lang-remaining 10 30 "test") "남은 시간은 10/30초 by test"))))

(deftest stateful-test
  (testing "send-remaining"
    (with-redefs-fn {#'current-time! (fn [] 0)
                     #'time-send!    (fn [message message-id] {:message message :message-id message-id})
                     #'edit-m!       (fn [message message-id] {:message message :message-id message-id})}
      #(do
         (is (= (send-remaining! {:mode  :relax
                                :started 0}
                                 "test")
                {:message "남은 시간은 30/30초 by test" :message-id nil}))
         (is (= (send-remaining! {:mode     :relax
                                  :started  0
                                :message-id 1}
                                 "test")
                {:message "남은 시간은 30/30초 by test" :message-id 1})))))
  (testing "remaining-each-10s"
    (with-redefs-fn {#'send-remaining! (fn [state _] state)
                     #'set-interval!   (fn [callback _] (callback))}
      #(do
         (is (= (remaining-each-10s! (fn [] {:state true})) {:state true}))))))


(deftest goto-x-test
  (testing "default"
    (when (config/get :3)
      (with-redefs [state-atom (atom {:timer      nil
                                      :interval   nil
                                      :started    nil
                                      :mode       nil
                                      :message-id nil})]

        (with-redefs-fn {#'current-time!       (fn [] 0)
                         #'remaining-each-10s! (fn [_] nil)
                         #'set-timeout!        (fn [_ _] nil)
                         #'send-m!             (fn [m] m)}
          #(do
             (goto-x! :pomodoro)
             (is (= (:mode (get-state!)) :pomodoro))
             (goto-x! :relax)
             (is (= (:mode (get-state!)) :relax))))))))

(deftest telegram-test
  (testing "send-m"
    (with-redefs-fn
      {#'telegram/send-text
       (fn
         ([token chat-id message] {:token token :chat-id chat-id :message message})
         ([token chat-id options message] {:token token :chat-id chat-id :options options :message message}))}
      #(let [without-options (send-m! "message")
             with-options (send-m! "message" {:option true})]
        (is (= (:token without-options) (config/get :token)))
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