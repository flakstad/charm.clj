(ns charm.components.textarea
  "Multi-line textarea component with cursor movement and editing."
  (:require
   [charm.message :as msg]
   [charm.render.cursor :as cursor]
   [charm.style.core :as style]
   [clojure.string :as str]))

(def ^:private default-keys
  {:character-forward   ["right" "ctrl+f"]
   :character-backward  ["left" "ctrl+b"]
   :line-up             ["up" "ctrl+p"]
   :line-down           ["down" "ctrl+n"]
   :line-start          ["home" "ctrl+a"]
   :line-end            ["end" "ctrl+e"]
   :delete-char-backward ["backspace" "ctrl+h"]
   :delete-char-forward ["delete" "del" "ctrl+d"]
   :delete-before-cursor ["ctrl+u"]
   :delete-after-cursor ["ctrl+k"]
   :new-line            ["enter" "ctrl+j"]
   :insert-tab          ["tab"]})

(defn- matches-binding?
  [m binding]
  (some #(msg/key-match? m %) binding))

(defn- clamp
  [n min-val max-val]
  (max min-val (min max-val n)))

(defn- split-lines*
  [s]
  (str/split (or s "") #"\n" -1))

(defn- line-col->index
  [text row col]
  (let [lines (vec (split-lines* text))
        row (clamp (long (or row 0)) 0 (max 0 (dec (count lines))))
        line (nth lines row "")
        col (clamp (long (or col 0)) 0 (count line))]
    (loop [r 0
           idx 0]
      (if (>= r row)
        (+ idx col)
        (recur (inc r)
               (+ idx (count (nth lines r "")) 1))))))

(defn- index->line-col
  [text idx]
  (let [text (or text "")
        idx (clamp (long (or idx 0)) 0 (count text))
        lines (vec (split-lines* text))]
    (loop [row 0
           rem idx]
      (let [line (nth lines row "")
            line-len (count line)
            last-row? (= row (dec (count lines)))]
        (if (or last-row?
                (<= rem line-len))
          [row rem]
          (recur (inc row)
                 (- rem (inc line-len))))))))

(defn- cursor-row-col
  [input]
  (index->line-col (:value input) (:cursor-index input)))

(defn- set-cursor-index*
  [input idx]
  (assoc input
         :cursor-index (clamp (long (or idx 0)) 0 (count (or (:value input) "")))
         :preferred-column nil))

(defn- set-cursor-by-row-col
  [input row col preserve-preferred?]
  (let [idx (line-col->index (:value input) row col)
        input (assoc input :cursor-index idx)]
    (if preserve-preferred?
      input
      (assoc input :preferred-column nil))))

(defn- ensure-visible
  [input]
  (let [{:keys [width height]} input
        [row col] (cursor-row-col input)
        y (long (or (:y-offset input) 0))
        x (long (or (:x-offset input) 0))
        y (if (pos? (long (or height 0)))
            (cond
              (< row y) row
              (>= row (+ y height)) (max 0 (- row height -1))
              :else y)
            0)
        x (if (pos? (long (or width 0)))
            (cond
              (< col x) col
              (>= col (+ x width)) (max 0 (- col width -1))
              :else x)
            0)]
    (assoc input
           :y-offset (max 0 y)
           :x-offset (max 0 x))))

(defn textarea
  "Create a textarea component.

   Options:
     :value            - Initial text value (default \"\")
     :placeholder      - Placeholder text when empty
     :char-limit       - Maximum characters (0 = unlimited)
     :width            - Visible text width (0 = unlimited)
     :height           - Visible line count (0 = unlimited)
     :show-line-numbers - Show line numbers (default false)
     :focused          - Start focused (default true)
     :text-style       - Style for text
     :placeholder-style - Style for placeholder
     :cursor-style     - Style for cursor
     :line-number-style - Style for line numbers
     :id               - Unique ID"
  [& {:keys [value placeholder char-limit width height show-line-numbers
             focused text-style placeholder-style cursor-style line-number-style id]
      :or {value ""
           char-limit 0
           width 0
           height 0
           show-line-numbers false
           focused true
           id (rand-int 1000000)}}]
  (let [value (or value "")]
    (-> {:type :textarea
         :id id
         :value value
         :cursor-index (count value)
         :preferred-column nil
         :y-offset 0
         :x-offset 0
         :char-limit (max 0 (long char-limit))
         :width (max 0 (long width))
         :height (max 0 (long height))
         :show-line-numbers (boolean show-line-numbers)
         :placeholder placeholder
         :focused (boolean focused)
         :text-style text-style
         :placeholder-style (or placeholder-style (style/style :fg 240))
         ;; Explicit cursor colors are more reliable than reverse-video spaces across terminals.
         :cursor-style (or cursor-style (style/style :bg 27 :fg 255 :bold true))
         :line-number-style (or line-number-style (style/style :fg 240))
         :keys default-keys}
        ensure-visible)))

(defn value
  [input]
  (or (:value input) ""))

(defn set-value
  [input v]
  (let [v (or v "")
        char-limit (long (or (:char-limit input) 0))
        v (if (and (pos? char-limit) (> (count v) char-limit))
            (subs v 0 char-limit)
            v)]
    (-> input
        (assoc :value v
               :cursor-index (count v)
               :preferred-column nil)
        ensure-visible)))

(defn cursor-index
  [input]
  (long (or (:cursor-index input) 0)))

(defn set-cursor-index
  [input idx]
  (-> input
      (set-cursor-index* idx)
      ensure-visible))

(defn cursor-row
  [input]
  (first (cursor-row-col input)))

(defn cursor-column
  [input]
  (second (cursor-row-col input)))

(defn focus
  [input]
  (assoc input :focused true))

(defn blur
  [input]
  (assoc input :focused false))

(defn focused?
  [input]
  (true? (:focused input)))

(defn reset
  [input]
  (assoc input
         :value ""
         :cursor-index 0
         :preferred-column nil
         :x-offset 0
         :y-offset 0))

(defn- insert-text
  [input s]
  (let [text (or (:value input) "")
        idx (clamp (long (or (:cursor-index input) 0)) 0 (count text))
        s (or s "")
        s (apply str (filter #(or (>= (int %) 32)
                                  (= % \tab)
                                  (= % \newline))
                             s))
        char-limit (long (or (:char-limit input) 0))
        allowed (if (pos? char-limit)
                  (max 0 (- char-limit (count text)))
                  (count s))
        s (if (< allowed (count s))
            (subs s 0 allowed)
            s)]
    (if (str/blank? (str/replace s #"\s+" ""))
      (if (seq s)
        (-> input
            (assoc :value (str (subs text 0 idx) s (subs text idx))
                   :cursor-index (+ idx (count s))
                   :preferred-column nil)
            ensure-visible)
        input)
      (-> input
          (assoc :value (str (subs text 0 idx) s (subs text idx))
                 :cursor-index (+ idx (count s))
                 :preferred-column nil)
          ensure-visible))))

(defn- delete-char-backward
  [input]
  (let [text (or (:value input) "")
        idx (clamp (long (or (:cursor-index input) 0)) 0 (count text))]
    (if (pos? idx)
      (-> input
          (assoc :value (str (subs text 0 (dec idx)) (subs text idx))
                 :cursor-index (dec idx)
                 :preferred-column nil)
          ensure-visible)
      input)))

(defn- delete-char-forward
  [input]
  (let [text (or (:value input) "")
        idx (clamp (long (or (:cursor-index input) 0)) 0 (count text))]
    (if (< idx (count text))
      (-> input
          (assoc :value (str (subs text 0 idx) (subs text (inc idx)))
                 :preferred-column nil)
          ensure-visible)
      input)))

(defn- delete-before-cursor
  [input]
  (let [text (or (:value input) "")
        idx (clamp (long (or (:cursor-index input) 0)) 0 (count text))
        [row _col] (index->line-col text idx)
        line-start (line-col->index text row 0)]
    (if (> idx line-start)
      (-> input
          (assoc :value (str (subs text 0 line-start) (subs text idx))
                 :cursor-index line-start
                 :preferred-column nil)
          ensure-visible)
      input)))

(defn- delete-after-cursor
  [input]
  (let [text (or (:value input) "")
        idx (clamp (long (or (:cursor-index input) 0)) 0 (count text))
        [row _col] (index->line-col text idx)
        line-end (line-col->index text row (count (nth (split-lines* text) row "")))]
    (if (< idx line-end)
      (-> input
          (assoc :value (str (subs text 0 idx) (subs text line-end))
                 :preferred-column nil)
          ensure-visible)
      input)))

(defn- move-left
  [input]
  (-> input
      (set-cursor-index* (dec (cursor-index input)))
      ensure-visible))

(defn- move-right
  [input]
  (-> input
      (set-cursor-index* (inc (cursor-index input)))
      ensure-visible))

(defn- move-line-start
  [input]
  (let [[row _col] (cursor-row-col input)]
    (-> input
        (set-cursor-by-row-col row 0 false)
        ensure-visible)))

(defn- move-line-end
  [input]
  (let [[row _col] (cursor-row-col input)
        line-len (count (nth (split-lines* (:value input)) row ""))]
    (-> input
        (set-cursor-by-row-col row line-len false)
        ensure-visible)))

(defn- move-up
  [input]
  (let [[row col] (cursor-row-col input)
        target-row (max 0 (dec row))
        preferred (long (or (:preferred-column input) col))
        line-len (count (nth (split-lines* (:value input)) target-row ""))]
    (-> input
        (set-cursor-by-row-col target-row (min preferred line-len) true)
        (assoc :preferred-column preferred)
        ensure-visible)))

(defn- move-down
  [input]
  (let [lines (split-lines* (:value input))
        [row col] (cursor-row-col input)
        target-row (min (max 0 (dec (count lines))) (inc row))
        preferred (long (or (:preferred-column input) col))
        line-len (count (nth lines target-row ""))]
    (-> input
        (set-cursor-by-row-col target-row (min preferred line-len) true)
        (assoc :preferred-column preferred)
        ensure-visible)))

(defn textarea-update
  "Update textarea state based on a message.
   Returns [new-textarea cmd] or [textarea nil] if message is not handled."
  [input msg]
  (if-not (:focused input)
    [input nil]
    (let [keys (:keys input)
          next (cond
                 (matches-binding? msg (:character-backward keys))
                 (move-left input)

                 (matches-binding? msg (:character-forward keys))
                 (move-right input)

                 (matches-binding? msg (:line-up keys))
                 (move-up input)

                 (matches-binding? msg (:line-down keys))
                 (move-down input)

                 (matches-binding? msg (:line-start keys))
                 (move-line-start input)

                 (matches-binding? msg (:line-end keys))
                 (move-line-end input)

                 (matches-binding? msg (:delete-char-backward keys))
                 (delete-char-backward input)

                 (matches-binding? msg (:delete-char-forward keys))
                 (delete-char-forward input)

                 (matches-binding? msg (:delete-before-cursor keys))
                 (delete-before-cursor input)

                 (matches-binding? msg (:delete-after-cursor keys))
                 (delete-after-cursor input)

                 (matches-binding? msg (:new-line keys))
                 (insert-text input "\n")

                 (matches-binding? msg (:insert-tab keys))
                 (insert-text input "\t")

                 (and (msg/key-press? msg)
                      (string? (:key msg))
                      (not (:ctrl msg))
                      (not (:alt msg)))
                 (insert-text input (:key msg))

                 :else input)]
      [next nil])))

(defn- line-slice
  [line x-offset width]
  (let [line (or line "")
        start (clamp (long (or x-offset 0)) 0 (count line))
        line (subs line start)
        width (long (or width 0))]
    (if (pos? width)
      (if (> (count line) width)
        (subs line 0 width)
        line)
      line)))

(defn- render-cursor
  [line cursor-col width text-style focused]
  (let [line (or line "")
        cursor-col (clamp (long (or cursor-col 0)) 0 (count line))
        width (long (or width 0))
        n (count line)
        at-eol? (= cursor-col n)
        full-row? (and at-eol? (pos? width) (>= n width))
        before-end (cond
                     full-row? (max 0 (dec n))
                     at-eol? n
                     :else cursor-col)
        before (subs line 0 before-end)
        cursor-char (cond
                      full-row? (subs line (max 0 (dec n)) n)
                      (< cursor-col n) (subs line cursor-col (inc cursor-col))
                      :else " ")
        after (cond
                full-row? ""
                (< cursor-col n) (subs line (inc cursor-col))
                :else "")
        before (if text-style (style/render text-style before) before)
        cursor-char (if focused
                      (cursor/mark cursor-char)
                      (if text-style (style/render text-style cursor-char) cursor-char))
        after (if text-style (style/render text-style after) after)]
    (str before cursor-char after)))

(defn textarea-view
  "Render textarea to a string."
  [input]
  (let [{:keys [value placeholder focused width height y-offset x-offset
                show-line-numbers text-style placeholder-style cursor-style line-number-style]} input
        text (or value "")
        lines (vec (split-lines* text))
        [cursor-row cursor-col] (index->line-col text (:cursor-index input))
        total-lines (max 1 (count lines))
        digits (max 2 (count (str total-lines)))
        y-offset (max 0 (long (or y-offset 0)))
        visible-end (if (pos? (long (or height 0)))
                      (min (count lines) (+ y-offset height))
                      (count lines))
        visible-lines (if (seq lines)
                        (subvec lines (min y-offset (count lines)) visible-end)
                        [""])
        placeholder? (and (str/blank? text)
                          (some? placeholder)
                          (not (str/blank? placeholder)))]
    (if placeholder?
      (let [raw (line-slice (str placeholder) x-offset width)
            rendered (if focused
                       (render-cursor raw 0 width text-style true)
                       (if placeholder-style
                         (style/render placeholder-style raw)
                         raw))
            prefix (when show-line-numbers
                     (let [label (format (str "%" digits "d ") 1)]
                       (if line-number-style
                         (style/render line-number-style label)
                         label)))]
        (if focused
          (cursor/render-cursor-markers (str (or prefix "") rendered) cursor-style)
          (str (or prefix "") rendered)))
      (let [s (->> visible-lines
                   (map-indexed
                     (fn [i raw-line]
                       (let [row (+ y-offset i)
                             line (line-slice raw-line x-offset width)
                             cursor-col' (- cursor-col (long (or x-offset 0)))
                             line (if (= row cursor-row)
                                    (render-cursor line cursor-col' width text-style focused)
                                    (if text-style (style/render text-style line) line))
                             prefix (when show-line-numbers
                                      (let [label (format (str "%" digits "d ") (inc row))]
                                        (if line-number-style
                                          (style/render line-number-style label)
                                          label)))]
                         (str (or prefix "") line))))
                   (str/join "\n"))]
        (if focused
          (cursor/render-cursor-markers s cursor-style)
          s)))))

(defn textarea-init
  [input]
  [input nil])
