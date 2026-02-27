# Markdown API

`charm.clj` exposes a minimal markdown renderer tuned for terminal output.

## `render-markdown`

Render markdown to a single string.

```clojure
(charm/render-markdown "# Hello\n\n- one\n- two")
```

## `markdown-lines`

Render markdown to a vector of lines.

```clojure
(charm/markdown-lines md {:width 72
                          :heading-color 81
                          :compact? false})
```

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `:width` | int | `80` | Target wrap width |
| `:heading-color` | int/map | `81` | Heading text color (ANSI-256 code or color map) |
| `:styles` | map | `nil` | Optional style overrides |
| `:compact?` | boolean | `false` | Skip blank spacer lines between blocks |

### Supported Markdown

- Headings (`#` ... `######`)
- Paragraphs
- Unordered and ordered lists
- Blockquotes (`>`)
- Fenced code blocks (```)
- Inline links and emphasis normalization
