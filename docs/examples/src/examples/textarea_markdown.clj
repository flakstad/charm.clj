(ns examples.textarea-markdown
  "Textarea + markdown preview demo.

   Run from docs/examples:
     clj -M -m examples.textarea-markdown
     bb textarea-markdown"
  (:require
   [charm.core :as charm]))

(def title-style
  (charm/style :fg charm/cyan :bold true))

(def hint-style
  (charm/style :fg 243))

(def cursor-style
  (charm/style :bg (charm/ansi 27) :fg (charm/ansi 255) :bold true))

(def ^:private sample-markdown
  (str "# Textarea + Markdown Demo\n\n"
       "This sample is designed to exercise renderer behavior.\n\n\n"
       "## Inline Formatting\n"
       "Inline: **bold**, *italic*, ~~strikethrough~~, and `code`.\n\n"
       "## Lists\n"
       "- item one\n"
       "- item two\n\n"
       "## Blockquote Wrapping\n"
       "> This quote is intentionally long so continuation rows should keep the quote prefix while wrapping across multiple lines in the preview pane.\n"))

(defn- clamp-wrap-width
  [state wrap-width]
  (let [max-wrap (max 20 (- (long (or (:width state) 100)) 12))]
    (assoc state :wrap-width (max 20 (min max-wrap (long wrap-width))))))

(defn- set-editor-size
  [editor width height]
  (let [w (max 20 (long width))
        h (max 6 (long height))
        editor (assoc editor :width w :height h)]
    ;; Re-apply cursor index so the textarea recomputes visibility bounds.
    (charm/textarea-set-cursor-index editor (charm/textarea-cursor-index editor))))

(defn- resize-state
  [state width height]
  (let [width (max 60 (long (or width 100)))
        height (max 18 (long (or height 30)))
        pane-width (max 24 (quot (- width 6) 2))
        pane-height (max 8 (- height 8))
        target-wrap (max 20 (- pane-width 4))
        state (if (some? (:wrap-width state))
                state
                (assoc state :wrap-width target-wrap))]
    (-> state
        (assoc :width width :height height)
        (update :editor set-editor-size pane-width pane-height)
        (clamp-wrap-width (:wrap-width state)))))

(defn init
  []
  (let [editor (-> (charm/textarea :value sample-markdown
                                   :show-line-numbers true
                                   :cursor-style cursor-style
                                   :focused true)
                   ;; Keep cursor on an obvious visible character on boot.
                   (charm/textarea-set-cursor-index 2))]
    [(resize-state {:width 100
                    :height 30
                    :editor editor}
                   100
                   30)
     nil]))

(defn update-fn
  [state msg]
  (cond
    (or (charm/key-match? msg "ctrl+c")
        (charm/key-match? msg "q"))
    [state charm/quit-cmd]

    (charm/window-size? msg)
    [(resize-state state (:width msg) (:height msg)) nil]

    (or (charm/key-match? msg "l")
        (charm/key-match? msg "+")
        (charm/key-match? msg "="))
    [(clamp-wrap-width state (+ 2 (long (:wrap-width state)))) nil]

    (or (charm/key-match? msg "h")
        (charm/key-match? msg "-")
        (charm/key-match? msg "_"))
    [(clamp-wrap-width state (- (long (:wrap-width state)) 2)) nil]

    :else
    (let [[editor cmd] (charm/textarea-update (:editor state) msg)]
      [(assoc state :editor editor) cmd])))

(defn view
  [state]
  (let [{:keys [width height editor wrap-width]} state
        pane-width (max 24 (quot (- width 6) 2))
        pane-height (max 8 (- height 8))
        editor-view (charm/textarea-view editor)
        markdown-view (charm/render-markdown (charm/textarea-value editor)
                                             {:width wrap-width})
        cursor-summary (format "cursor row=%d col=%d idx=%d"
                               (charm/textarea-cursor-row editor)
                               (charm/textarea-cursor-column editor)
                               (charm/textarea-cursor-index editor))
        editor-panel (charm/render (charm/style :border charm/rounded-border
                                                :padding [0 1]
                                                :width pane-width
                                                :height pane-height
                                                :valign :top)
                                   editor-view)
        preview-panel (charm/render (charm/style :border charm/rounded-border
                                                 :padding [0 1]
                                                 :width pane-width
                                                 :height pane-height
                                                 :valign :top)
                                    markdown-view)]
    (str (charm/render title-style "Textarea + Markdown Preview") "\n"
         (charm/render hint-style "Type on the left. h/l or +/-: wrap  q/ctrl+c: quit") "\n"
         (charm/render hint-style (str "wrap-width=" wrap-width "  " cursor-summary)) "\n\n"
         (charm/join-horizontal :top editor-panel "  " preview-panel))))

(defn -main
  [& _args]
  (charm/run {:init init
              :update update-fn
              :view view
              :alt-screen true
              :render-mode :raw}))
