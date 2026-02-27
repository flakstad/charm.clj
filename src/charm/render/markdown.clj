(ns charm.render.markdown
  "Minimal Markdown-to-terminal renderer.

   Focused on predictable rendering for TUI usage:
   - headings
   - paragraphs
   - unordered / ordered lists
   - blockquotes
   - fenced code blocks
   - inline links, bold/italic/strikethrough/code"
  (:require
   [charm.ansi.width :as aw]
   [charm.style.core :as style]
   [clojure.string :as str]))

(def ^:private ansi-sgr-re #"\u001b\[[0-9;]*m")

(defn- ->color
  [v]
  (cond
    (nil? v) nil
    (map? v) v
    (number? v) (style/ansi256 (long v))
    :else nil))

(defn- default-styles
  [{:keys [heading-color]}]
  (let [heading-fg (or (->color heading-color) (style/ansi256 81))
        code-bg (style/ansi256 236)
        code-fg (style/ansi256 255)]
    {:heading {1 (style/style :bold true :underline true :fg heading-fg)
               2 (style/style :bold true :fg heading-fg)
               3 (style/style :bold true :fg heading-fg)
               4 (style/style :bold true :fg heading-fg)
               5 (style/style :bold true :fg heading-fg)
               6 (style/style :bold true :fg heading-fg)}
     :paragraph nil
     :list nil
     ;; Keep quote text at normal foreground by default.
     :quote nil
     :code (style/style :bg code-bg :fg code-fg)
     :rule (style/style :fg (style/ansi256 245))
     :inline {:code (style/style :bg code-bg :fg code-fg)
              :strong (style/style :bold true)
              :emphasis (style/style :italic true)}}))

(defn- merge-styles
  [base styles]
  (merge-with (fn [a b]
                (if (and (map? a) (map? b))
                  (merge a b)
                  (or b a)))
              base
              (or styles {})))

(defn- normalize-links
  [s]
  (-> (or s "")
      ;; Images and links become "label (target)"
      (str/replace #"\!\[([^\]]*)\]\(([^)]+)\)" "$1 ($2)")
      (str/replace #"\[([^\]]+)\]\(([^)]+)\)" "$1 ($2)")))

(def ^:private inline-token-re
  #"`([^`]+)`|\*\*([^*]+)\*\*|__([^_]+)__|~~([^~]+)~~|\*([^*\n]+)\*|_([^_\n]+)_")

(defn- apply-strikethrough
  [s]
  (let [s (or s "")]
    (str "\u001b[9m"
         (str/replace s ansi-sgr-re "$0\u001b[9m")
         "\u001b[29m")))

(defn- render-inline
  [s styles]
  (let [s (normalize-links s)
        strong-style (get-in styles [:inline :strong])
        emph-style (get-in styles [:inline :emphasis])
        code-style (get-in styles [:inline :code])
        matcher (re-matcher inline-token-re s)]
    (loop [cursor 0
           out (StringBuilder.)]
      (if (.find matcher)
        (let [start (.start matcher)
              end (.end matcher)
              _ (.append out (subs s cursor start))
              rendered (cond
                         (some? (.group matcher 1))
                         (let [code (.group matcher 1)]
                           (if code-style
                             (style/render code-style code)
                             code))

                         (some? (.group matcher 2))
                         (let [strong (.group matcher 2)]
                           (if strong-style
                             (style/render strong-style strong)
                             strong))

                         (some? (.group matcher 3))
                         (let [strong (.group matcher 3)]
                           (if strong-style
                             (style/render strong-style strong)
                             strong))

                         (some? (.group matcher 4))
                         (apply-strikethrough (.group matcher 4))

                         (some? (.group matcher 5))
                         (let [emph (.group matcher 5)]
                           (if emph-style
                             (style/render emph-style emph)
                             emph))

                         :else
                         (let [emph (.group matcher 6)]
                           (if emph-style
                             (style/render emph-style emph)
                             emph)))]
          (.append out rendered)
          (recur end out))
        (do
          (.append out (subs s cursor))
          (str out))))))

(defn- push-paragraph
  [blocks paragraph]
  (let [text (->> paragraph
                  (map str/trim)
                  (remove str/blank?)
                  (str/join " ")
                  str/trim)]
    (if (str/blank? text)
      blocks
      (conj blocks {:type :paragraph
                    :text text}))))

(defn- parse-blocks
  [markdown]
  (let [lines (str/split (or markdown "") #"\r?\n")]
    (loop [xs lines
           blocks []
           paragraph []
           in-code? false
           code-lines []]
      (if (empty? xs)
        (cond-> (push-paragraph blocks paragraph)
          in-code? (conj {:type :code
                          :lines code-lines}))
        (let [line (first xs)
              tail (rest xs)]
          (if in-code?
            (if (re-matches #"^\s*```.*$" line)
              (recur tail
                     (conj (push-paragraph blocks paragraph)
                           {:type :code
                            :lines code-lines})
                     []
                     false
                     [])
              (recur tail blocks paragraph true (conj code-lines line)))
            (cond
              (re-matches #"^\s*```.*$" line)
              (recur tail (push-paragraph blocks paragraph) [] true [])

              (str/blank? (str/trim line))
              (recur tail
                     (conj (push-paragraph blocks paragraph)
                           {:type :blank})
                     []
                     false
                     code-lines)

              (re-matches #"^\s*(\*{3,}|-{3,}|_{3,})\s*$" line)
              (recur tail
                     (conj (push-paragraph blocks paragraph) {:type :rule})
                     []
                     false
                     code-lines)

              :else
              (if-let [[_ hashes text] (re-matches #"^\s*(#{1,6})\s+(.+)\s*$" line)]
                (recur tail
                       (conj (push-paragraph blocks paragraph)
                             {:type :heading
                              :level (count hashes)
                              :text text})
                       []
                       false
                       code-lines)
                (if-let [[_ text] (re-matches #"^\s*>\s?(.*)$" line)]
                  (recur tail
                         (conj (push-paragraph blocks paragraph)
                               {:type :quote
                                :text text})
                         []
                         false
                         code-lines)
                  (if-let [[_ ws n text] (re-matches #"^(\s*)(\d+)[\.\)]\s+(.+)$" line)]
                    (recur tail
                           (conj (push-paragraph blocks paragraph)
                                 {:type :list
                                  :ordered? true
                                  :n n
                                  :depth (quot (count ws) 2)
                                  :text text})
                           []
                           false
                           code-lines)
                    (if-let [[_ ws text] (re-matches #"^(\s*)[-*+]\s+(.+)$" line)]
                      (recur tail
                             (conj (push-paragraph blocks paragraph)
                                   {:type :list
                                    :ordered? false
                                    :depth (quot (count ws) 2)
                                    :text text})
                             []
                             false
                             code-lines)
                      (recur tail blocks (conj paragraph line) false code-lines))))))))))))

(defn- wrap-words
  [text width]
  (let [text (str/trim (or text ""))]
    (if (or (not (pos? (long (or width 0))))
            (<= (aw/string-width text) width))
      [text]
      (let [words (vec (remove str/blank? (str/split text #"\s+")))]
        (if (empty? words)
          [""]
          (loop [remaining words
                 line ""
                 out []]
            (if (empty? remaining)
              (conj out line)
              (let [w (first remaining)
                    candidate (if (str/blank? line) w (str line " " w))]
                (if (<= (aw/string-width candidate) width)
                  (recur (subvec remaining 1) candidate out)
                  (if (str/blank? line)
                    ;; Single long token, hard truncate.
                    (recur (subvec remaining 1) "" (conj out (aw/truncate w width :tail "")))
                    (recur remaining "" (conj out line))))))))))))

(defn- wrap-prefixed
  [prefix text width & {:keys [continuation-prefix]}]
  (let [prefix (or prefix "")
        p-width (aw/string-width prefix)
        avail (if (pos? (long (or width 0)))
                (max 1 (- width p-width))
                0)
        lines (wrap-words text avail)
        continuation (or continuation-prefix (apply str (repeat p-width " ")))]
    (if (empty? lines)
      [prefix]
      (vec (cons (str prefix (first lines))
                 (map #(str continuation %) (rest lines)))))))

(defn- style-prefix
  [st]
  (let [rendered (style/render st "X")]
    (if-let [[_ prefix] (re-find #"^(.*)X(?:\u001b\[0m)?$" rendered)]
      prefix
      "")))

(defn- reapply-style-after-resets
  [s st]
  (let [prefix (style-prefix st)]
    (if (str/blank? prefix)
      (or s "")
      (str/replace (or s "") "\u001b[0m" (str "\u001b[0m" prefix)))))

(defn- style-lines
  [lines st]
  (if st
    (mapv #(reapply-style-after-resets (style/render st %) st) lines)
    (vec lines)))

(defn- block->lines
  [block width styles]
  (let [kind (:type block)]
    (case kind
      :heading
      (style-lines
       (wrap-words (render-inline (:text block) styles) width)
       (get-in styles [:heading (long (or (:level block) 1))]))

      :paragraph
      (style-lines
       (wrap-words (render-inline (:text block) styles) width)
       (:paragraph styles))

      :quote
      (style-lines
       (wrap-prefixed "│ " (render-inline (:text block) styles) width
                      :continuation-prefix "│ ")
       (:quote styles))

      :list
      (let [depth (max 0 (long (or (:depth block) 0)))
            bullet (if (true? (:ordered? block))
                     (str (or (:n block) "1") ". ")
                     "• ")
            prefix (str (apply str (repeat (* 2 depth) " "))
                        bullet)]
        (style-lines
         (wrap-prefixed prefix (render-inline (:text block) styles) width)
         (:list styles)))

      :code
      (style-lines
       (if (seq (:lines block))
         (mapv #(str "    " %) (:lines block))
         ["    "])
       (:code styles))

      :rule
      (style-lines
       [(apply str (repeat (max 3 (or width 3)) "─"))]
       (:rule styles))

      :blank
      [""]

      [])))

(defn markdown-lines
  "Render markdown into terminal lines.

   Options:
     :width   - Target wrap width (default 80)
     :heading-color - Heading foreground color (ANSI-256 number or color map)
     :styles  - Override style map (same shape as internal default-styles)
     :compact? - Remove blank lines between blocks (default false)"
  ([markdown]
   (markdown-lines markdown {}))
  ([markdown {:keys [width heading-color styles compact?]
              :or {width 80
                   compact? false}}]
   (let [width (max 1 (long width))
         blocks (parse-blocks markdown)
         blocks (if compact?
                  (remove #(= :blank (:type %)) blocks)
                  blocks)
         styles (merge-styles (default-styles {:heading-color heading-color}) styles)
         out (->> blocks
                  (mapcat #(block->lines % width styles))
                  vec)]
     (if (seq out) out [""]))))

(defn render-markdown
  "Render markdown into a single string."
  ([markdown]
   (render-markdown markdown {}))
  ([markdown opts]
   (str/join "\n" (markdown-lines markdown opts))))
