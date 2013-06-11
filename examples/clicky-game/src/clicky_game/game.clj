(ns clicky-game.game
  (:require [noisesmith.pachinko.dataflow :as >]))

(def act
  (>/>> [message [id [status room posessions :as player] x y]]
        -| views players |-
        -= [old-x old-y] (:clicked status)
           old-x (or old-x 0)
           old-y (or old-y 0) =-
        ...
        ::>/+ [:players (assoc players id [{:clicked [x y]} room posessions])
               :views (assoc views id [["rects" [["#000000" [0 0 800 800]]
                                                 ["#FFFFFF" [old-x old-y 10 10]]
                                                 ["#FF0000" [x y 10 10]]]]])]))
          

(def move-maker
  (>/>> [message [id x y]]
        -| players objects rooms |-
        -= [status room posessions :as player] (get players id) =-
        ...
        ::act [id player x y]))
 
(defn run
  [world player-id x y]
  (>/run world [::move-maker [player-id x y]])
  (get (>/answer world :views) player-id))

;; ["images" [[(+ x 10) (+ y 10) "brushy-turtle.gif"]]]])

(defn init
  []
  (>/run (>/debug-world)
         [::>/>> [::move-maker move-maker
                  ::act act]
          ::>/+ [:players {} :objects {} :rooms {} :views {}]]))