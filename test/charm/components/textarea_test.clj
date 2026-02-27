(ns charm.components.textarea-test
  (:require
   [charm.ansi.width :as aw]
   [charm.components.textarea :as ta]
   [charm.message :as msg]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(deftest textarea-creation-test
  (testing "create textarea with defaults"
    (let [input (ta/textarea)]
      (is (= :textarea (:type input)))
      (is (= "" (ta/value input)))
      (is (= 0 (ta/cursor-index input)))
      (is (ta/focused? input))
      (is (= 0 (:width input)))
      (is (= 0 (:height input)))))

  (testing "create textarea with options"
    (let [input (ta/textarea :value "hello\nworld"
                             :width 40
                             :height 10
                             :show-line-numbers true)]
      (is (= "hello\nworld" (ta/value input)))
      (is (= 11 (ta/cursor-index input)))
      (is (= 40 (:width input)))
      (is (= 10 (:height input)))
      (is (true? (:show-line-numbers input))))))

(deftest textarea-focus-test
  (testing "focus and blur"
    (let [input (ta/textarea :focused false)]
      (is (false? (ta/focused? input)))
      (is (ta/focused? (ta/focus input)))
      (is (false? (ta/focused? (ta/blur (ta/focus input)))))))

  (testing "reset clears value and cursor"
    (let [input (ta/reset (ta/textarea :value "abc\ndef"))]
      (is (= "" (ta/value input)))
      (is (= 0 (ta/cursor-index input))))))

(deftest textarea-editing-test
  (testing "typing inserts text"
    (let [[input _] (ta/textarea-update (ta/textarea :value "hlo")
                                        (msg/key-press "el"))]
      (is (= "hloel" (ta/value input)))
      (is (= 5 (ta/cursor-index input)))))

  (testing "enter inserts newline"
    (let [[input _] (ta/textarea-update (ta/textarea :value "hello")
                                        (msg/key-press :enter))]
      (is (= "hello\n" (ta/value input)))
      (is (= 6 (ta/cursor-index input)))))

  (testing "tab inserts tab character"
    (let [[input _] (ta/textarea-update (ta/textarea :value "a")
                                        (msg/key-press :tab))]
      (is (= "a\t" (ta/value input)))))

  (testing "backspace deletes previous character"
    (let [[input _] (ta/textarea-update (ta/textarea :value "hello")
                                        (msg/key-press "backspace"))]
      (is (= "hell" (ta/value input)))
      (is (= 4 (ta/cursor-index input)))))

  (testing "delete removes character at cursor"
    (let [input (ta/set-cursor-index (ta/textarea :value "hello") 1)
          [input _] (ta/textarea-update input (msg/key-press :delete))]
      (is (= "hllo" (ta/value input)))
      (is (= 1 (ta/cursor-index input)))))

  (testing "ctrl+u deletes back to line start"
    (let [input (-> (ta/textarea :value "abc\ndef")
                    (ta/set-cursor-index 6))
          [input _] (ta/textarea-update input (msg/key-press "u" :ctrl true))]
      (is (= "abc\nf" (ta/value input)))
      (is (= 4 (ta/cursor-index input)))))

  (testing "ctrl+k deletes to line end"
    (let [input (-> (ta/textarea :value "abc\ndef")
                    (ta/set-cursor-index 5))
          [input _] (ta/textarea-update input (msg/key-press "k" :ctrl true))]
      (is (= "abc\nd" (ta/value input)))
      (is (= 5 (ta/cursor-index input))))))

(deftest textarea-cursor-navigation-test
  (testing "left and right movement"
    (let [input (-> (ta/textarea :value "abc")
                    (ta/set-cursor-index 1))
          [left _] (ta/textarea-update input (msg/key-press :left))
          [right _] (ta/textarea-update left (msg/key-press :right))]
      (is (= 0 (ta/cursor-index left)))
      (is (= 1 (ta/cursor-index right)))))

  (testing "home and end move within current line"
    (let [input (-> (ta/textarea :value "abc\ndef")
                    (ta/set-cursor-index 5))
          [home _] (ta/textarea-update input (msg/key-press :home))
          [end _] (ta/textarea-update home (msg/key-press :end))]
      (is (= 4 (ta/cursor-index home)))
      (is (= 7 (ta/cursor-index end)))))

  (testing "up and down preserve preferred column"
      (let [input (-> (ta/textarea :value "abcd\nxy\nmnopq")
                    (ta/set-cursor-index 3))
          [down _] (ta/textarea-update input (msg/key-press :down))
          [down2 _] (ta/textarea-update down (msg/key-press :down))
          [up _] (ta/textarea-update down2 (msg/key-press :up))]
      (is (= [1 2] [(ta/cursor-row down) (ta/cursor-column down)]))
      (is (= [2 3] [(ta/cursor-row down2) (ta/cursor-column down2)]))
      (is (= [1 2] [(ta/cursor-row up) (ta/cursor-column up)])))))

(deftest textarea-char-limit-test
  (testing "char limit applies to set-value and typing"
    (let [input (-> (ta/textarea :char-limit 5)
                    (ta/set-value "abcdefgh"))
          [input2 _] (ta/textarea-update input (msg/key-press "zz"))]
      (is (= "abcde" (ta/value input)))
      (is (= "abcde" (ta/value input2))))))

(deftest textarea-view-test
  (testing "view renders multiline content"
    (let [input (-> (ta/textarea :value "a\nb\nc" :focused false)
                    (ta/set-cursor-index 1))
          view (ta/textarea-view input)]
      (is (str/includes? view "a"))
      (is (str/includes? view "b"))
      (is (str/includes? view "c"))))

  (testing "view shows placeholder when empty"
    (let [input (ta/textarea :placeholder "Write..." :focused false)
          view (ta/textarea-view input)]
      (is (str/includes? view "Write..."))))

  (testing "view can show line numbers"
    (let [input (ta/textarea :value "a\nb"
                             :show-line-numbers true
                             :focused false)
          view (ta/textarea-view input)]
      (is (str/includes? view "1 "))
      (is (str/includes? view "2 "))))

  (testing "cursor remains visible at end of width-constrained line"
    (let [input (-> (ta/textarea :value "abcd"
                                 :width 4
                                 :focused true)
                    (ta/set-cursor-index 4))
          view (ta/textarea-view input)
          plain (aw/strip-ansi view)]
      (is (re-find #"\u001b\[[0-9;]*m" view))
      (is (= 4 (count plain)))
      (is (not= "abcd" plain))))

  (testing "cursor at end of line stays visible on unconstrained rows"
    (let [input (-> (ta/textarea :value "abc"
                                 :width 10
                                 :focused true)
                    (ta/set-cursor-index 3))
          view (ta/textarea-view input)
          plain (aw/strip-ansi view)]
      (is (re-find #"\u001b\[[0-9;]*m" view))
      (is (= "abc\u00a0" plain))
      (is (not (str/includes? view "\u0001")))
      (is (not (str/includes? view "\u0002"))))))

(deftest textarea-init-test
  (testing "init returns textarea and nil command"
    (let [input (ta/textarea)
          [next-input cmd] (ta/textarea-init input)]
      (is (= input next-input))
      (is (nil? cmd)))))
