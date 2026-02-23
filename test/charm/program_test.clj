(ns charm.program-test
  (:require [clojure.core.async :as a]
            [clojure.test :refer [deftest is testing]]
            [charm.program :as p]
            [charm.message :as msg]))

;; Note: Full program tests require terminal interaction.
;; These tests focus on command and message helper functions.

(deftest cmd-test
  (testing "creates command from function"
    (let [c (p/cmd (fn [] (msg/quit)))]
      (is (= :cmd (:type c)))
      (is (fn? (:fn c))))))

(deftest batch-test
  (testing "combines multiple commands"
    (let [c1 (p/cmd (fn [] :a))
          c2 (p/cmd (fn [] :b))
          batch (p/batch c1 c2)]
      (is (= :batch (:type batch)))
      (is (= 2 (count (:cmds batch))))))

  (testing "filters nil commands"
    (let [c1 (p/cmd (fn [] :a))
          batch (p/batch c1 nil nil)]
      (is (= 1 (count (:cmds batch)))))))

(deftest sequence-cmds-test
  (testing "creates sequence of commands"
    (let [c1 (p/cmd (fn [] :a))
          c2 (p/cmd (fn [] :b))
          seq (p/sequence-cmds c1 c2)]
      (is (= :sequence (:type seq)))
      (is (= 2 (count (:cmds seq)))))))

(deftest quit-cmd-test
  (testing "quit-cmd is a command"
    (is (= :cmd (:type p/quit-cmd)))
    (is (fn? (:fn p/quit-cmd))))

  (testing "quit-cmd produces quit message"
    (let [result ((:fn p/quit-cmd))]
      (is (msg/quit? result)))))

(deftest window-size-msg-test
  (testing "creates window size message"
    (let [m (p/window-size-msg 80 24)]
      (is (= :window-size (:type m)))
      (is (= 80 (:width m)))
      (is (= 24 (:height m))))))

(deftest next-msg-prioritizes-ready-queue
  (testing "next-msg!! returns immediately when a message is already queued"
    (let [ch (a/chan 4)
          _ (a/>!! ch {:type :test})
          t0 (System/nanoTime)
          m (#'p/next-msg!! ch 100)
          dt-ms (/ (- (System/nanoTime) t0) 1000000.0)]
      (is (= :test (:type m)))
      ;; Should not wait for the full timeout when queue is non-empty.
      (is (< dt-ms 20.0))
      (a/close! ch))))

(deftest drain-msg-burst-processes-queued-messages
  (testing "drains already queued messages in one burst and returns final state"
    (let [ch (a/chan 8)
          _ (a/>!! ch {:type :msg :delta 2})
          _ (a/>!! ch {:type :msg :delta 3})
          running? (atom true)
          exec-calls (atom 0)
          resize-calls (atom [])
          [next-state should-render?]
          (#'p/drain-msg-burst!
           {:total 0}
           {:type :msg :delta 1}
           running?
           ch
           (fn [state msg]
             [(update state :total + (long (:delta msg))) nil])
           (fn [_cmd _msg-chan]
             (swap! exec-calls inc))
           (fn [w h]
             (swap! resize-calls conj [w h])))]
      (is (= {:total 6} next-state))
      (is (= true should-render?))
      (is (= true @running?))
      (is (= 3 @exec-calls))
      (is (= [] @resize-calls))
      (is (nil? (a/poll! ch)))
      (a/close! ch))))

(deftest drain-msg-burst-handles-window-size
  (testing "window-size messages trigger resize callback and continue draining"
    (let [ch (a/chan 4)
          _ (a/>!! ch {:type :msg :delta 4})
          running? (atom true)
          resize-calls (atom [])
          [next-state should-render?]
          (#'p/drain-msg-burst!
           {:total 0}
           {:type :window-size :width 80 :height 24}
           running?
           ch
           (fn [state msg]
             (if (= :msg (:type msg))
               [(update state :total + (long (:delta msg))) nil]
               [state nil]))
           (fn [_cmd _msg-chan] nil)
           (fn [w h]
             (swap! resize-calls conj [w h])))]
      (is (= {:total 4} next-state))
      (is (= true should-render?))
      (is (= [[80 24]] @resize-calls))
      (a/close! ch))))
