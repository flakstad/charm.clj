# Textarea

Multi-line text entry component with cursor movement, line navigation, and editing.

## Quick Example

```clojure
(require '[charm.core :as charm])

(def editor (charm/textarea :value "Line one\nLine two"
                            :height 8
                            :width 64
                            :show-line-numbers true))

;; In update:
(let [[editor cmd] (charm/textarea-update editor msg)]
  [editor cmd])

;; In view:
(charm/textarea-view editor)
```

## Creation Options

```clojure
(charm/textarea & options)
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `:value` | string | `""` | Initial multi-line text |
| `:placeholder` | string | `nil` | Text shown when empty |
| `:char-limit` | int | `0` | Max characters (0 = unlimited) |
| `:width` | int | `0` | Visible text width (0 = unlimited) |
| `:height` | int | `0` | Visible line count (0 = unlimited) |
| `:show-line-numbers` | boolean | `false` | Render line numbers |
| `:focused` | boolean | `true` | Start focused |
| `:text-style` | style | `nil` | Style for text lines |
| `:placeholder-style` | style | gray | Style for placeholder |
| `:cursor-style` | style | reverse | Style for cursor |
| `:line-number-style` | style | gray | Style for line numbers |
| `:id` | any | random | Unique identifier |

## Key Bindings

| Action | Keys |
|--------|------|
| Move left/right | `Left`, `Right`, `Ctrl+B`, `Ctrl+F` |
| Move up/down | `Up`, `Down`, `Ctrl+P`, `Ctrl+N` |
| Line start/end | `Home`, `End`, `Ctrl+A`, `Ctrl+E` |
| Backspace | `Backspace`, `Ctrl+H` |
| Delete | `Delete`, `Ctrl+D` |
| Delete to line start | `Ctrl+U` |
| Delete to line end | `Ctrl+K` |
| Newline | `Enter` |
| Insert tab | `Tab` |

## Functions

### `textarea-update`

```clojure
(charm/textarea-update editor msg) ; => [new-editor cmd]
```

### `textarea-view`

```clojure
(charm/textarea-view editor) ; => multi-line string
```

### Value/Cursor Helpers

```clojure
(charm/textarea-value editor)
(charm/textarea-set-value editor "new text")
(charm/textarea-cursor-index editor)
(charm/textarea-set-cursor-index editor 10)
(charm/textarea-cursor-row editor)
(charm/textarea-cursor-column editor)
```

### Focus Helpers

```clojure
(charm/textarea-focus editor)
(charm/textarea-blur editor)
(charm/textarea-focused? editor)
(charm/textarea-reset editor)
```
