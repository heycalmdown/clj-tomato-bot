(ns tomato.core-test
  (:require [clojure.test :refer :all]
            [tomato.core :refer :all]))

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
    (with-redefs-fn {#'current-time (fn [] 1000)}
      #(do
        (is (= (current-time) 1000))
        (is (= (elapsed-time 500) 500)))))
  (testing "remaining"
    (with-redefs-fn {#'current-time (fn [] (mins 1))}
      #(do
        (is (= (remaining-secs {:mode :relax :started (mins 1)}) 30))
        (is (= (remaining-secs {:mode :relax :started (- (mins 1) (secs 25))}) 5))))))

(deftest sent-message-test
  (testing "message-id"
    (let [message {:ok true,
                   :result {:message_id 1094,
                            :from {:id 259347720, :first_name "tomato", :username "k_tomato_bot"},
                            :chat {:id 54879231, :first_name "Kei", :last_name "Son",
                                   :username "heycalmdown", :type "private"},
                            :date 1477809856,
                            :text "haha"}}]
      (is (= (pluck-message-id message) 1094)))))

;(deftest goto-x-text
;  (testing ":pomodoro"
;    (goto-x :pomodoro)
;    (is (= (:mode @state) :pomodoro))))
