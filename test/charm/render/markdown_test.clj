(ns charm.render.markdown-test
  (:require
   [charm.ansi.width :as aw]
   [charm.render.markdown :as md]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(deftest heading-and-paragraph-test
  (let [lines (md/markdown-lines "# Title\n\nHello world.")
        plain (mapv aw/strip-ansi lines)]
    (testing "renders heading and paragraph text with preserved blank line"
      (is (= ["Title" "" "Hello world."] plain))
      (is (re-find #"\u001b\[[0-9;]*m" (first lines))))))

(deftest list-rendering-test
  (let [out (md/render-markdown "- one\n- two\n1. first\n2. second")]
    (testing "renders unordered and ordered list markers"
      (is (str/includes? (aw/strip-ansi out) "• one"))
      (is (str/includes? (aw/strip-ansi out) "• two"))
      (is (str/includes? (aw/strip-ansi out) "1. first"))
      (is (str/includes? (aw/strip-ansi out) "2. second")))))

(deftest quote-rendering-test
  (let [lines (md/markdown-lines "> quoted line that should wrap with quote prefix on each wrapped row"
                                 {:width 22})
        plain (mapv aw/strip-ansi lines)]
    (testing "renders blockquote with quote prefix on all wrapped lines"
      (is (> (count plain) 1))
      (is (every? #(str/starts-with? % "│ ") plain)))))

(deftest code-block-rendering-test
  (let [out (md/render-markdown "```clj\n(+ 1 2)\n```")]
    (testing "renders fenced code block with indentation"
      (is (str/includes? (aw/strip-ansi out) "    (+ 1 2)")))))

(deftest inline-formatting-test
  (let [out (md/render-markdown "Visit [Ro](https://ro.app) and `code` with **bold**, *italic*, and ~~strike~~.")
        plain (aw/strip-ansi out)]
    (testing "renders links and inline formatting"
      (is (str/includes? plain "Ro (https://ro.app)"))
      (is (str/includes? plain "code"))
      (is (str/includes? plain "bold"))
      (is (str/includes? plain "italic"))
      (is (str/includes? plain "strike"))
      (is (not (str/includes? plain "[Ro]")))
      (is (not (str/includes? plain "**bold**")))
      (is (not (str/includes? plain "*italic*")))
      (is (not (str/includes? plain "~~strike~~")))
      (is (re-find #"\u001b\[[0-9;]*m" out))
      (is (str/includes? out "\u001b[9m")))))

(deftest wrapping-test
  (let [lines (md/markdown-lines "alpha beta gamma delta epsilon zeta" {:width 12})]
    (testing "wraps to requested width"
      (is (every? #(<= (aw/string-width %) 12) lines))
      (is (> (count lines) 1)))))

(deftest preserves-multiple-blank-lines-test
  (let [lines (md/markdown-lines "alpha\n\n\nbeta")
        plain (mapv aw/strip-ansi lines)]
    (testing "preserves explicit blank lines in source markdown"
      (is (= ["alpha" "" "" "beta"] plain)))))

(deftest compact-mode-test
  (let [normal (mapv aw/strip-ansi (md/markdown-lines "# H\n\npara"))
        compact (mapv aw/strip-ansi (md/markdown-lines "# H\n\npara" {:compact? true}))]
    (testing "compact mode removes explicit blank lines"
      (is (= ["H" "" "para"] normal))
      (is (= ["H" "para"] compact))
      (is (not-any? str/blank? compact)))))

(deftest heading-color-option-test
  (let [out (md/render-markdown "# H" {:heading-color 196})]
    (testing "heading-color option controls heading foreground color"
      (is (re-find #"\u001b\[[0-9;]*38;5;196" out))
      (is (str/includes? (aw/strip-ansi out) "H")))))
