(ns clicky-game.game
  (:require [noisesmith.pachinko.dataflow :as >]))

(defn click-inside?
  [-x -y]
  (fn [[identifier [x y] [w h] & ignored]]
    (and (< x -x (+ x w))
         (< y -y (+ y h))
         identifier)))

(def it (comp first (partial filter identity)))

(defn room-image
  [room]
  [0 0 (str "room-" room ".png")])

(defn object-image
  [object x y]
  [x y (str "object-" object ".png")])

(defn inventory-object-image
  [object place]
  (object-image 650 (* (inc place) 80)))

(defn room-object-image
  [[object [x y] & ignored]]
  (object-image x y))

(def act
  (>/>> [message [id [status room posessions :as player] x y]]
        -| views players rooms |-
        -= [old-x old-y] (or (:clicked status) [0 0])
           inside? (click-inside? x y)
           room-map (get rooms room)
           grabbed-object (it (map inside? (:objects room-map)))
           posessions (if grabbed-object
                        (conj posessions grabbed-object)
                        posessions)
           new-room (or (when-not grabbed-object
                          (it (map inside? (:exits room-map))))
                        room 1)
           room-img (room-image new-room)
           object-images (map-indexed inventory-object-image posessions)
           images (conj object-images room-img) =-
        ...
        ::room (and grabbed-object [:remove room grabbed-object id])
        ::>/+ [:players (assoc players id [{:clicked [x y]}
                                            new-room
                                            posessions])
               :views (assoc views id [["rects" [["#000000" [0 0 800 800]]]]
                                       ["images" images]
                                       ["rects" [["#FFFFFF" [old-x old-y 40 40]]
                                                 ["#FF0000" [x y 40 40]]]]])]))

(def move-maker
  (>/>> [message [id x y]]
        -| players objects rooms |-
        -= [status room posessions :as player] (get players id) =-
        ...
        ::act [id player x y]))

(def room-do
  (>/>> [message [action room x id]]
        -| views rooms |-
        -= room-map (get rooms room)
           room-map (if (= action :remove)
                      (update-in room-map [:objects]
                                 (partial remove (comp  #{x} first)))
                      room-map)
           player-view (concat (get views id)
                               (map room-object-image (:objects room-map)))
           rooms (assoc rooms room room-map) =-
        ...
        ::>/+ [:views (assoc views id player-view)
               :rooms rooms]))

(defn run
  [world player-id x y]
  (>/run world [::move-maker [player-id x y]])
  (get (>/answer world :views) player-id))

;; ["images" [[(+ x 10) (+ y 10) "brushy-turtle.gif"]]]])

(defn init
  []
  (>/run (>/debug-world)
         [::>/>> [::move-maker move-maker
                  ::act act
                  ::room room-do]
          ::>/+ [:players {}
                 :objects {}
                 :views {}
                 :rooms {1 {:exits [[6 [13 3] [86 70]]
                                    [2 [579 19] [88 64]]]}
                         2 {:exits [[1 [25 1] [212 94]]
                                    [3 [500 7] [193 117]]]}
                         3 {:exits [[2 [307 2] [124 141]]]}
                         4 {:exits [[3 [21 185] [507 314]]]}
                         5 {:exits [[4 [232 237] [209 245]]]}
                         6 {:exits [[5 [10 19] [170 185]]
                                    [1 [310 211] [347 202]]]}}]]))
