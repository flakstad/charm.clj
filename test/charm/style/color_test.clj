(ns charm.style.color-test
  (:require [clojure.test :refer [deftest is testing]]
            [charm.style.color :as c]))

(deftest ansi-color-test
  (testing "creates ANSI colors from numbers"
    (is (= {:type :ansi :code 1} (c/ansi 1)))
    (is (= {:type :ansi :code 0} (c/ansi 0))))

  (testing "creates ANSI colors from keywords"
    (is (= {:type :ansi :code 1} (c/ansi :red)))
    (is (= {:type :ansi :code 4} (c/ansi :blue)))
    (is (= {:type :ansi :code 9} (c/ansi :bright-red)))))

(deftest ansi256-color-test
  (testing "creates ANSI 256 colors"
    (is (= {:type :ansi256 :code 196} (c/ansi256 196)))
    (is (= {:type :ansi256 :code 0} (c/ansi256 0)))
    (is (= {:type :ansi256 :code 255} (c/ansi256 255)))))

(deftest rgb-color-test
  (testing "creates RGB colors"
    (is (= {:type :rgb :r 255 :g 0 :b 0} (c/rgb 255 0 0)))
    (is (= {:type :rgb :r 0 :g 255 :b 0} (c/rgb 0 255 0)))))

(deftest hex-color-test
  (testing "parses hex colors with #"
    (is (= {:type :rgb :r 255 :g 0 :b 0} (c/hex "#ff0000")))
    (is (= {:type :rgb :r 0 :g 255 :b 0} (c/hex "#00ff00"))))

  (testing "parses hex colors without #"
    (is (= {:type :rgb :r 255 :g 255 :b 255} (c/hex "ffffff")))
    (is (= {:type :rgb :r 0 :g 0 :b 0} (c/hex "000000")))))

(deftest styled-str-test
  (testing "applies foreground color"
    (is (= "\u001b[31mx\u001b[0m" (c/styled-str "x" :fg (c/ansi :red))))
    (is (= "\u001b[34mx\u001b[0m" (c/styled-str "x" :fg (c/ansi :blue)))))

  (testing "applies ANSI 256 foreground"
    (is (= "\u001b[38;5;196mx\u001b[0m" (c/styled-str "x" :fg (c/ansi256 196)))))

  (testing "applies RGB foreground"
    (is (= "\u001b[38;5;196mx\u001b[0m" (c/styled-str "x" :fg (c/rgb 255 0 0)))))

  (testing "applies background color"
    (is (= "\u001b[41mx\u001b[0m" (c/styled-str "x" :bg (c/ansi :red))))
    (is (= "\u001b[44mx\u001b[0m" (c/styled-str "x" :bg (c/ansi :blue)))))

  (testing "applies ANSI 256 background"
    (is (= "\u001b[48;5;196mx\u001b[0m" (c/styled-str "x" :bg (c/ansi256 196)))))

  (testing "applies both foreground and background"
    (is (= "\u001b[31;44mx\u001b[0m" (c/styled-str "x" :fg (c/ansi :red) :bg (c/ansi :blue)))))

  (testing "returns unchanged for no colors"
    (is (= "x" (c/styled-str "x")))
    (is (= "x" (c/styled-str "x" :fg nil :bg nil)))))

(deftest rgb->ansi256-test
  (testing "converts RGB to ANSI 256"
    ;; Pure red should map to color cube
    (let [result (c/rgb->ansi256 {:r 255 :g 0 :b 0})]
      (is (= :ansi256 (:type result))))
    ;; Gray should map to grayscale ramp
    (let [result (c/rgb->ansi256 {:r 128 :g 128 :b 128})]
      (is (= :ansi256 (:type result)))
      (is (>= (:code result) 232)))))  ; Grayscale starts at 232

(deftest predefined-colors-test
  (testing "predefined colors are available"
    (is (= {:type :ansi :code 0} c/black))
    (is (= {:type :ansi :code 1} c/red))
    (is (= {:type :ansi :code 2} c/green))
    (is (= {:type :ansi :code 7} c/white))
    (is (= {:type :ansi :code 9} c/bright-red))))
