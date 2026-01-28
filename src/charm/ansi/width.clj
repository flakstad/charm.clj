(ns charm.ansi.width
  "Text width calculation for terminal display.

   Handles:
   - ANSI escape sequences (zero width)
   - Wide characters (CJK, emojis = 2 cells)
   - Combining characters (zero width)
   - Grapheme clusters (emoji sequences)"
  (:require
   [clojure.string :as str])
  (:import
   [java.text BreakIterator]
   [java.lang Character$UnicodeBlock]))

;; ---------------------------------------------------------------------------
;; ANSI Escape Sequence Handling
;; ---------------------------------------------------------------------------

(def ^:private ansi-pattern
  "Regex pattern matching ANSI escape sequences.
   Matches: CSI sequences, OSC sequences, and simple escapes."
  #"\x1b(?:\[[0-9;?]*[A-Za-z]|\][^\x07]*(?:\x07|\x1b\\)|\[[^\x1b]*|[^\[])|\x1b")

(defn strip-ansi
  "Remove ANSI escape sequences from a string."
  [s]
  (if (nil? s)
    ""
    (str/replace s ansi-pattern "")))

;; ---------------------------------------------------------------------------
;; Character Width Calculation
;; ---------------------------------------------------------------------------

(defn- all-ascii?
  "Check if a string contains only ASCII characters (codepoints < 128)."
  [s]
  (every? #(< (int %) 128) s))

(def ^:private wide-blocks
  "Set of Unicode blocks that contain wide (2-cell) characters."
  #{Character$UnicodeBlock/CJK_UNIFIED_IDEOGRAPHS
    Character$UnicodeBlock/CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
    Character$UnicodeBlock/CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
    Character$UnicodeBlock/CJK_COMPATIBILITY
    Character$UnicodeBlock/CJK_COMPATIBILITY_FORMS
    Character$UnicodeBlock/CJK_COMPATIBILITY_IDEOGRAPHS
    Character$UnicodeBlock/CJK_RADICALS_SUPPLEMENT
    Character$UnicodeBlock/CJK_SYMBOLS_AND_PUNCTUATION
    Character$UnicodeBlock/ENCLOSED_CJK_LETTERS_AND_MONTHS
    Character$UnicodeBlock/HIRAGANA
    Character$UnicodeBlock/KATAKANA
    Character$UnicodeBlock/KATAKANA_PHONETIC_EXTENSIONS
    Character$UnicodeBlock/HANGUL_SYLLABLES
    Character$UnicodeBlock/HANGUL_JAMO
    Character$UnicodeBlock/HANGUL_COMPATIBILITY_JAMO
    Character$UnicodeBlock/HALFWIDTH_AND_FULLWIDTH_FORMS
    Character$UnicodeBlock/EMOTICONS
    Character$UnicodeBlock/MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS
    Character$UnicodeBlock/TRANSPORT_AND_MAP_SYMBOLS
    Character$UnicodeBlock/SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
    Character$UnicodeBlock/DINGBATS})

(def ^:private zero-width-codepoints
  "Set of zero-width character code points."
  #{0x200B   ; ZERO WIDTH SPACE
    0x200C   ; ZERO WIDTH NON-JOINER
    0x200D   ; ZERO WIDTH JOINER
    0xFEFF}) ; ZERO WIDTH NO-BREAK SPACE (BOM)

(defn- zero-width-char?
  "Check if a code point is a zero-width character."
  [^long cp]
  (let [char-type (Character/getType cp)]
    (or
     ;; Combining marks
     (= char-type Character/NON_SPACING_MARK)
     (= char-type Character/ENCLOSING_MARK)
     (= char-type Character/COMBINING_SPACING_MARK)
     ;; Zero-width characters
     (contains? zero-width-codepoints cp)
     ;; Control characters
     (= char-type Character/CONTROL))))

(defn- wide-char?
  "Check if a code point is a wide (2-cell) character."
  [^long cp]
  (or (contains? wide-blocks (Character$UnicodeBlock/of cp))
      (Character/isSupplementaryCodePoint cp)))

(defn char-width
  "Get the display width of a single code point.
   Returns 0, 1, or 2."
  [^long cp]
  (cond
    (zero-width-char? cp) 0
    (wide-char? cp) 2
    :else 1))

;; ---------------------------------------------------------------------------
;; Grapheme Cluster Handling
;; ---------------------------------------------------------------------------

(defn graphemes
  "Split a string into grapheme clusters.
   A grapheme cluster is what users perceive as a single character,
   which may consist of multiple Unicode code points (e.g., emoji sequences)."
  [^String s]
  (when (and s (pos? (count s)))
    (let [iter (BreakIterator/getCharacterInstance)]
      (.setText iter s)
      (loop [start (.first iter)
             clusters []]
        (let [end (.next iter)]
          (if (= end BreakIterator/DONE)
            clusters
            (recur end (conj clusters (.substring s start end)))))))))

(defn grapheme-width
  "Get the display width of a grapheme cluster."
  [^String grapheme]
  (if (or (nil? grapheme) (empty? grapheme))
    0
    (let [cp (.codePointAt grapheme 0)]
      (char-width cp))))

;; ---------------------------------------------------------------------------
;; String Width Measurement
;; ---------------------------------------------------------------------------

(defn string-width
  "Measure the display width of a string in terminal cells.

   - ANSI escape sequences have zero width
   - Wide characters (CJK, emojis) count as 2 cells
   - Combining characters count as 0 cells

   Example:
     (string-width \"hello\")     ; => 5
     (string-width \"你好\")       ; => 4 (2 wide chars)
     (string-width \"\\033[31mhi\") ; => 2 (ANSI ignored)"
  [s]
  (if (or (nil? s) (empty? s))
    0
    (let [stripped (strip-ansi s)]
      (if (all-ascii? stripped) ;; fast path for babashka
        (count stripped)
        (let [clusters (graphemes stripped)]
          (transduce (map grapheme-width) + 0 clusters))))))

;; ---------------------------------------------------------------------------
;; Truncation
;; ---------------------------------------------------------------------------

(defn truncate
  "Truncate a string to fit within a given display width.

   Options:
     :tail - String to append when truncated (default \"...\")

   The tail is included in the width calculation.

   Example:
     (truncate \"hello world\" 8)           ; => \"hello...\"
     (truncate \"hello world\" 8 :tail \"…\") ; => \"hello w…\""
  [s width & {:keys [tail] :or {tail "..."}}]
  (if (or (nil? s) (<= (string-width s) width))
    s
    (let [tail-width (string-width tail)
          target-width (- width tail-width)]
      (if (neg? target-width)
        ""
        (let [stripped (strip-ansi s)
              clusters (graphemes stripped)]
          (loop [result []
                 remaining clusters
                 current-width (long 0)]
            (if (empty? remaining)
              (str (apply str result) tail)
              (let [g (first remaining)
                    gw (grapheme-width g)
                    new-width (+ current-width gw)]
                (if (> new-width target-width)
                  (str (apply str result) tail)
                  (recur (conj result g)
                         (rest remaining)
                         (long new-width)))))))))))

(defn pad-right
  "Pad a string on the right to reach a target display width."
  [s width & {:keys [char] :or {char \space}}]
  (let [current (string-width s)
        needed (- width current)]
    (if (pos? needed)
      (str s (apply str (repeat needed char)))
      s)))

(defn pad-left
  "Pad a string on the left to reach a target display width."
  [s width & {:keys [char] :or {char \space}}]
  (let [current (string-width s)
        needed (- width current)]
    (if (pos? needed)
      (str (apply str (repeat needed char)) s)
      s)))
