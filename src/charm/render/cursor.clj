(ns charm.render.cursor
  "Cursor marker rendering.

   Mirrors the app-level strategy:
   - inject cursor markers in text
   - resolve markers to styled cells at final render step"
  (:require
   [charm.style.core :as style]
   [clojure.string :as str]))

(def ^:private cursor-start "\u0001")
(def ^:private cursor-end "\u0002")
(def ^:private cursor-marker-re #"\u0001([\s\S]?)\u0002")
(def ^:private nbsp "\u00a0")

(def ^:private default-cursor-style
  (style/style :bg 27 :fg 255 :bold true))

(defn- normalize-cell
  [cell]
  (let [cell (cond
               (string? cell) cell
               (nil? cell) " "
               :else (str cell))
        cell (if (seq cell) (subs cell 0 1) " ")]
    ;; Keep end-of-line cursors visible even when terminals collapse trailing spaces.
    (if (= cell " ")
      nbsp
      cell)))

(defn- style-cursor-cell
  [cursor-style cell]
  (let [cell (normalize-cell cell)
        styled (style/render cursor-style cell)]
    ;; Fallback: if style rendering is a no-op, force reverse video so the cursor is visible.
    (if (= styled cell)
      (str "\u001b[7m" cell "\u001b[27m")
      styled)))

(defn mark
  "Mark a single-cell cursor slot for later styling."
  [cell]
  (str cursor-start (normalize-cell cell) cursor-end))

(defn render-cursor-markers
  "Replace cursor markers in a string with styled cursor cells."
  ([s]
   (render-cursor-markers s default-cursor-style))
  ([s cursor-style]
   (str/replace (or s "")
                cursor-marker-re
                (fn [[_ cell]]
                  (style-cursor-cell cursor-style cell)))))
