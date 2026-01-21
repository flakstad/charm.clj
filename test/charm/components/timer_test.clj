(ns charm.components.timer-test
  (:require [clojure.test :refer [deftest testing is]]
            [charm.components.timer :as timer]))

(deftest timer-creation-test
  (testing "create timer with defaults"
    (let [t (timer/timer)]
      (is (= :timer (:type t)))
      (is (= 0 (timer/timeout t)))
      (is (= 1000 (timer/interval t)))
      (is (:running t))))

  (testing "create timer with options"
    (let [t (timer/timer :timeout 5000 :interval 500 :running false)]
      (is (= 5000 (timer/timeout t)))
      (is (= 500 (timer/interval t)))
      (is (not (:running t))))))

(deftest timer-accessors-test
  (testing "set-timeout"
    (let [t (timer/set-timeout (timer/timer) 10000)]
      (is (= 10000 (timer/timeout t)))))

  (testing "set-interval"
    (let [t (timer/set-interval (timer/timer) 500)]
      (is (= 500 (timer/interval t)))))

  (testing "set-interval minimum 1"
    (let [t (timer/set-interval (timer/timer) 0)]
      (is (= 1 (timer/interval t))))))

(deftest timer-state-test
  (testing "running? when running and has time"
    (is (timer/running? (timer/timer :timeout 5000 :running true))))

  (testing "running? false when timeout is 0"
    (is (not (timer/running? (timer/timer :timeout 0 :running true)))))

  (testing "running? false when not running"
    (is (not (timer/running? (timer/timer :timeout 5000 :running false)))))

  (testing "timed-out? when timeout is 0"
    (is (timer/timed-out? (timer/timer :timeout 0))))

  (testing "timed-out? when timeout is negative"
    (is (timer/timed-out? (timer/timer :timeout -100))))

  (testing "timed-out? false when has time"
    (is (not (timer/timed-out? (timer/timer :timeout 5000))))))

(deftest tick-msg-test
  (testing "create tick message"
    (let [msg (timer/tick-msg 42 1)]
      (is (= :timer-tick (:type msg)))
      (is (= 42 (:timer-id msg)))
      (is (= 1 (:tag msg)))))

  (testing "tick-msg? predicate"
    (is (timer/tick-msg? {:type :timer-tick}))
    (is (not (timer/tick-msg? {:type :other})))))

(deftest timer-control-test
  (testing "start returns timer and cmd"
    (let [t (timer/timer :timeout 5000 :running false)
          [new-t cmd] (timer/start t)]
      (is (:running new-t))
      (is (some? cmd))))

  (testing "stop returns timer and nil"
    (let [t (timer/timer :timeout 5000)
          [new-t cmd] (timer/stop t)]
      (is (not (:running new-t)))
      (is (nil? cmd))))

  (testing "toggle from running to stopped"
    (let [t (timer/timer :timeout 5000 :running true)
          [new-t _] (timer/toggle t)]
      (is (not (:running new-t)))))

  (testing "toggle from stopped to running"
    (let [t (timer/timer :timeout 5000 :running false)
          [new-t _] (timer/toggle t)]
      (is (:running new-t))))

  (testing "reset"
    (let [t (timer/timer :timeout 1000)
          [new-t cmd] (timer/reset t 10000)]
      (is (= 10000 (timer/timeout new-t)))
      (is (:running new-t))
      (is (some? cmd)))))

(deftest timer-init-test
  (testing "init running timer returns cmd"
    (let [t (timer/timer :timeout 5000)
          [new-t cmd] (timer/timer-init t)]
      (is (= t new-t))
      (is (some? cmd))))

  (testing "init stopped timer returns nil cmd"
    (let [t (timer/timer :timeout 5000 :running false)
          [new-t cmd] (timer/timer-init t)]
      (is (= t new-t))
      (is (nil? cmd)))))

(deftest timer-update-test
  (testing "tick message decrements timeout"
    (let [t (timer/timer :timeout 5000 :interval 1000 :id 42)
          msg (timer/tick-msg 42 0)
          [new-t cmd] (timer/timer-update t msg)]
      (is (= 4000 (timer/timeout new-t)))
      (is (some? cmd))))

  (testing "tick message ignores wrong timer"
    (let [t (timer/timer :timeout 5000 :id 42)
          msg (timer/tick-msg 99 0)
          [new-t cmd] (timer/timer-update t msg)]
      (is (= 5000 (timer/timeout new-t)))
      (is (nil? cmd))))

  (testing "tick message ignores wrong tag"
    (let [t (timer/timer :timeout 5000 :id 42)
          msg (timer/tick-msg 42 5)
          [new-t cmd] (timer/timer-update t msg)]
      (is (= 5000 (timer/timeout new-t)))
      (is (nil? cmd))))

  (testing "tick stops at timeout"
    (let [t (timer/timer :timeout 1000 :interval 1000 :id 42)
          msg (timer/tick-msg 42 0)
          [new-t cmd] (timer/timer-update t msg)]
      (is (= 0 (timer/timeout new-t)))
      (is (not (:running new-t)))
      (is (nil? cmd)))))

(deftest timer-view-test
  (testing "view formats seconds"
    (let [t (timer/timer :timeout 5000)]
      (is (= "5s" (timer/timer-view t)))))

  (testing "view formats minutes and seconds"
    (let [t (timer/timer :timeout 125000)]
      (is (= "2:05" (timer/timer-view t)))))

  (testing "view formats hours"
    (let [t (timer/timer :timeout 3725000)]
      (is (= "1:02:05" (timer/timer-view t)))))

  (testing "view handles zero"
    (let [t (timer/timer :timeout 0)]
      (is (= "0s" (timer/timer-view t))))))

(deftest for-timer?-test
  (testing "for-timer? returns true for matching"
    (let [t (timer/timer :id 42)
          msg (timer/tick-msg 42 0)]
      (is (timer/for-timer? t msg))))

  (testing "for-timer? returns false for non-matching"
    (let [t (timer/timer :id 42)
          msg (timer/tick-msg 99 0)]
      (is (not (timer/for-timer? t msg))))))
