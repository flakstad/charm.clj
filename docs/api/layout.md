# Layout API

The layout API provides functions for combining and aligning text blocks.

## Joining Text Blocks

### join-horizontal

```clojure
(charm/join-horizontal position & texts)
```

Join multiple text blocks side by side.

**Position** specifies vertical alignment: `:top`, `:center`, or `:bottom`.

```clojure
(charm/join-horizontal :top "Left" "Right")
; LeftRight

(charm/join-horizontal :top
  "Line 1\nLine 2\nLine 3"
  "  |  "
  "A\nB")
; Line 1  |  A
; Line 2  |  B
; Line 3  |
```

**With styled boxes:**

```clojure
(def box1
  (charm/render
    (charm/style :border charm/rounded-border :padding [0 1])
    "Box 1"))

(def box2
  (charm/render
    (charm/style :border charm/rounded-border :padding [0 1])
    "Box 2"))

(charm/join-horizontal :top box1 "  " box2)
; ╭───────╮  ╭───────╮
; │ Box 1 │  │ Box 2 │
; ╰───────╯  ╰───────╯
```

**Vertical alignment:**

```clojure
;; Top aligned (default)
(charm/join-horizontal :top
  "Short"
  "Tall\ntext\nhere")
; ShortTall
;      text
;      here

;; Center aligned
(charm/join-horizontal :center
  "Short"
  "Tall\ntext\nhere")
;      Tall
; Shorttext
;      here

;; Bottom aligned
(charm/join-horizontal :bottom
  "Short"
  "Tall\ntext\nhere")
;      Tall
;      text
; Shorthere
```

### join-vertical

```clojure
(charm/join-vertical position & texts)
```

Join multiple text blocks vertically (stacked).

**Position** specifies horizontal alignment: `:left`, `:center`, or `:right`.

```clojure
(charm/join-vertical :left "Top" "Bottom")
; Top
; Bottom

(charm/join-vertical :center
  "Short"
  "Longer text"
  "Medium")
;   Short
; Longer text
;   Medium

(charm/join-vertical :right
  "Short"
  "Longer text"
  "Medium")
;       Short
; Longer text
;      Medium
```

**With styled boxes:**

```clojure
(def header
  (charm/render
    (charm/style :border charm/rounded-border :width 30 :align :center)
    "Header"))

(def content
  (charm/render
    (charm/style :border charm/normal-border :width 30)
    "Content goes here"))

(charm/join-vertical :left header content)
; ╭──────────────────────────────╮
; │           Header             │
; ╰──────────────────────────────╯
; ┌──────────────────────────────┐
; │Content goes here             │
; └──────────────────────────────┘
```

## Alignment Constants

```clojure
;; Horizontal alignment
charm/left
charm/center
charm/right

;; Vertical alignment
charm/top
charm/bottom
; charm/center (same constant)
```

## Building Layouts

### Two-Column Layout

```clojure
(defn two-column [left-content right-content]
  (charm/join-horizontal :top
    (charm/render
      (charm/style :width 30 :border charm/normal-border)
      left-content)
    "  "
    (charm/render
      (charm/style :width 30 :border charm/normal-border)
      right-content)))
```

### Header/Content/Footer Layout

```clojure
(defn page-layout [header content footer]
  (charm/join-vertical :left
    (charm/render
      (charm/style :width 60 :align :center :border charm/rounded-border)
      header)
    (charm/render
      (charm/style :width 60 :border charm/normal-border :padding [1 2])
      content)
    (charm/render
      (charm/style :width 60 :align :center :fg 240)
      footer)))
```

### Sidebar Layout

```clojure
(defn sidebar-layout [sidebar main-content]
  (charm/join-horizontal :top
    (charm/render
      (charm/style :width 20 :border charm/normal-border)
      sidebar)
    (charm/render
      (charm/style :width 50 :border charm/normal-border :padding [0 1])
      main-content)))
```

## Complete Example

```clojure
(ns my-app
  (:require [charm.core :as charm]))

(def title-style
  (charm/style :fg charm/cyan :bold true :align :center))

(def box-style
  (charm/style :border charm/rounded-border :padding [0 1]))

(def dim-style
  (charm/style :fg 240))

(defn render-sidebar [items selected]
  (charm/render
    (charm/style :border charm/normal-border :width 20)
    (clojure.string/join "\n"
      (map-indexed
        (fn [i item]
          (if (= i selected)
            (charm/render (charm/style :fg charm/cyan :bold true)
                         (str "> " item))
            (str "  " item)))
        items))))

(defn render-content [text]
  (charm/render
    (charm/style :border charm/rounded-border
                 :padding [1 2]
                 :width 40)
    text))

(defn render-help []
  (charm/render dim-style "j/k: navigate  q: quit"))

(defn view [state]
  (let [sidebar (render-sidebar (:items state) (:selected state))
        content (render-content (nth (:items state) (:selected state)))
        help (render-help)]
    (charm/join-vertical :left
      (charm/render title-style "My Application")
      ""
      (charm/join-horizontal :top sidebar "  " content)
      ""
      help)))
```

## Tips

### Spacing Between Elements

Use empty strings for spacing:

```clojure
;; Horizontal spacing
(charm/join-horizontal :top box1 "   " box2)  ; 3 spaces

;; Vertical spacing
(charm/join-vertical :left header "" "" content)  ; 2 blank lines
```

### Consistent Widths

Set explicit widths for alignment:

```clojure
(charm/join-vertical :left
  (charm/render (charm/style :width 40) "Row 1")
  (charm/render (charm/style :width 40) "Row 2")
  (charm/render (charm/style :width 40) "Row 3"))
```

### Nesting Layouts

Layouts can be nested:

```clojure
(charm/join-vertical :center
  header
  (charm/join-horizontal :top
    left-panel
    (charm/join-vertical :left
      top-right
      bottom-right))
  footer)
```
