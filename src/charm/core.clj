(ns charm.core
  "charm.clj - A Clojure TUI library inspired by Bubble Tea.

   This is the main entry point for charm.clj applications.

   Example usage:
   ```clojure
   (require '[charm.core :as charm])

   (defn update-fn [state msg]
     (cond
       (charm/key-match? msg \"k\") [(update state :count inc) nil]
       (charm/key-match? msg \"j\") [(update state :count dec) nil]
       (charm/key-match? msg \"q\") [state charm/quit-cmd]
       :else [state nil]))

   (defn view [state]
     (str \"Count: \" (:count state) \"\\n\\n(j/k to change, q to quit)\"))

   (charm/run {:init {:count 0}
               :update update-fn
               :view view})
   ```"
  (:require [charm.terminal :as term]
            [charm.message :as msg]
            [charm.program :as prog]
            [charm.style.core :as style]
            [charm.style.color :as color]
            [charm.style.border :as border]))

;; ---------------------------------------------------------------------------
;; Re-exported from charm.message
;; ---------------------------------------------------------------------------

(def key-press msg/key-press)
(def window-size msg/window-size)
(def quit msg/quit)
(def error msg/error)
(def mouse msg/mouse)
(def focus msg/focus)
(def blur msg/blur)

(def key-press? msg/key-press?)
(def window-size? msg/window-size?)
(def quit? msg/quit?)
(def error? msg/error?)
(def mouse? msg/mouse?)

(def key-match? msg/key-match?)
(def ctrl? msg/ctrl?)
(def alt? msg/alt?)
(def shift? msg/shift?)

;; ---------------------------------------------------------------------------
;; Re-exported from charm.terminal
;; ---------------------------------------------------------------------------

(def create-terminal term/create-terminal)
(def get-size term/get-size)

;; ---------------------------------------------------------------------------
;; Re-exported from charm.program
;; ---------------------------------------------------------------------------

(def cmd prog/cmd)
(def batch prog/batch)
(def sequence-cmds prog/sequence-cmds)
(def quit-cmd prog/quit-cmd)
(def run prog/run)
(def run-simple prog/run-simple)

;; ---------------------------------------------------------------------------
;; Re-exported from charm.style
;; ---------------------------------------------------------------------------

(def style style/style)
(def render style/render)
(def styled style/styled)

;; Colors
(def rgb style/rgb)
(def hex style/hex)
(def ansi style/ansi)
(def ansi256 style/ansi256)

(def black style/black)
(def red style/red)
(def green style/green)
(def yellow style/yellow)
(def blue style/blue)
(def magenta style/magenta)
(def cyan style/cyan)
(def white style/white)

;; Borders
(def normal-border style/normal-border)
(def rounded-border style/rounded-border)
(def thick-border style/thick-border)
(def double-border style/double-border)

;; Layout
(def join-horizontal style/join-horizontal)
(def join-vertical style/join-vertical)

;; ---------------------------------------------------------------------------
;; Convenience Macros
;; ---------------------------------------------------------------------------

(defmacro defprogram
  "Define a TUI program with init, update, and view functions.

   Usage:
     (defprogram my-app
       :init {:count 0}
       :update (fn [state msg] ...)
       :view (fn [state] ...))"
  [name & {:keys [init update view] :as opts}]
  `(def ~name
     {:init ~init
      :update ~update
      :view ~view}))
