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
    (is (= (session-alive? {:timer true}) true))))

(deftest goto-x-text
  (testing ":pomodoro"
    (goto-x :pomodoro)
    (is (= (:mode @state) :pomodoro))))
