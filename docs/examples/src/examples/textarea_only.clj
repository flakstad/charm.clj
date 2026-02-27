(ns examples.textarea-only
  "Textarea-only demo for isolated component testing.

   Run from docs/examples:
     clj -M -m examples.textarea-only
     bb textarea"
  (:require
   [charm.core :as charm]))

(def title-style
  (charm/style :fg charm/cyan :bold true))

(def hint-style
  (charm/style :fg 243))

(defn- set-editor-size
  [editor width height]
  (let [w (max 20 (long width))
        h (max 4 (long height))
        editor (assoc editor :width w :height h)]
    (charm/textarea-set-cursor-index editor (charm/textarea-cursor-index editor))))

(defn- resize-state
  [state width height]
  (let [width (max 60 (long (or width 100)))
        height (max 16 (long (or height 30)))
        editor-width (max 20 (- width 8))
        editor-height (max 4 (- height 10))]
    (-> state
        (assoc :width width :height height)
        (update :editor set-editor-size editor-width editor-height))))

(defn init
  []
  (let [editor (charm/textarea :value "Line one\nLine two"
                               :show-line-numbers true
                               :focused true)]
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

    :else
    (let [[editor cmd] (charm/textarea-update (:editor state) msg)]
      [(assoc state :editor editor) cmd])))

(defn view
  [state]
  (let [{:keys [editor]} state
        idx (charm/textarea-cursor-index editor)
        row (charm/textarea-cursor-row editor)
        col (charm/textarea-cursor-column editor)
        chars (count (charm/textarea-value editor))
        stats (format "idx=%d  row=%d  col=%d  chars=%d" idx row col chars)
        editor-view (charm/textarea-view editor)
        panel (charm/render (charm/style :border charm/rounded-border
                                         :padding [0 1]
                                         :valign :top)
                            editor-view)]
    (str (charm/render title-style "Textarea Component Demo") "\n"
         (charm/render hint-style "Type freely. q/ctrl+c quit.") "\n"
         (charm/render hint-style stats) "\n\n"
         panel)))

(defn -main
  [& _args]
  (charm/run {:init init
              :update update-fn
              :view view
              :alt-screen true
              :render-mode :raw}))
