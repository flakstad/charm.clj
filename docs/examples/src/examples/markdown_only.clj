(ns examples.markdown-only
  "Markdown-only demo for isolated renderer testing.

   Run from docs/examples:
     clj -M -m examples.markdown-only
     bb markdown"
  (:require
   [charm.core :as charm]))

(def title-style
  (charm/style :fg charm/green :bold true))

(def hint-style
  (charm/style :fg 243))

(def ^:private sample-markdown
  (str "# Markdown Demo\n\n"
       "Paragraph one.\n\n"
       "Paragraph two after multiple newlines.\n\n\n"
       "> Blockquote wrapping should keep prefix on continuation rows.\n\n"
       "- item one\n"
       "- item two\n\n"
       "Inline: **bold**, *italic*, ~~strikethrough~~, and `code`.\n"))

(defn- resize-state
  [state width height]
  (let [width (max 60 (long (or width 100)))
        height (max 16 (long (or height 30)))
        wrap-width (max 20 (min (or (:wrap-width state) 64) (- width 10)))]
    (assoc state
           :width width
           :height height
           :wrap-width wrap-width)))

(defn init
  []
  [(resize-state {:width 100
                  :height 30
                  :wrap-width 64}
                 100
                 30)
   nil])

(defn- clamp-wrap-width
  [state wrap-width]
  (let [max-wrap (max 20 (- (long (or (:width state) 100)) 10))]
    (assoc state :wrap-width (max 20 (min max-wrap (long wrap-width))))))

(defn update-fn
  [state msg]
  (cond
    (or (charm/key-match? msg "ctrl+c")
        (charm/key-match? msg "q"))
    [state charm/quit-cmd]

    (charm/window-size? msg)
    [(resize-state state (:width msg) (:height msg)) nil]

    (or (charm/key-match? msg "+")
        (charm/key-match? msg "=")
        (charm/key-match? msg "l"))
    [(clamp-wrap-width state (+ 2 (long (:wrap-width state)))) nil]

    (or (charm/key-match? msg "-")
        (charm/key-match? msg "_")
        (charm/key-match? msg "h"))
    [(clamp-wrap-width state (- (long (:wrap-width state)) 2)) nil]

    :else
    [state nil]))

(defn view
  [state]
  (let [{:keys [wrap-width]} state
        rendered (charm/render-markdown sample-markdown
                                        {:width wrap-width})
        panel (charm/render (charm/style :border charm/rounded-border
                                         :padding [0 1]
                                         :valign :top)
                            rendered)]
    (str (charm/render title-style "Markdown Renderer Demo") "\n"
         (charm/render hint-style "h/l or +/-: wrap width  q: quit") "\n"
         (charm/render hint-style (str "wrap-width=" wrap-width)) "\n\n"
         panel)))

(defn -main
  [& _args]
  (charm/run {:init init
              :update update-fn
              :view view
              :alt-screen true
              :render-mode :raw}))
