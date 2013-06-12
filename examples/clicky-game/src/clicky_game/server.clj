(ns clicky-game.server
  (:require [lamina.core :as lamina]
            [aleph.http :as aleph]
            [clojure.java.io :as io]
            [cheshire.core :as cheshire]
            [clojure.tools.nrepl.server :as nrepl]
            [clojure.string :as string]
            [clicky-game.game :as game]
            [noisesmith.pachinko.dataflow :as >]))

(defn static
  [request, filename, content-type]
  (let [image? (.startsWith content-type "image/")
        get-bin (fn []
                  (when-let [file (try (new java.io.FileInputStream
                                            (str "resources/public/" filename))
                                       (catch java.io.FileNotFoundException
                                           e nil))]
                    (with-open [input file
                                output (new java.io.ByteArrayOutputStream)]
                      (io/copy input output)
                      (.toByteArray output))))
        get-tdata (fn []
                    (when-let [resource (io/resource (str "public/" filename))]
                      (slurp resource)))
        body (if image? (get-bin) (get-tdata))
        status (if body 200 404)
        content-type (if body content-type "text/plain")
        body (or body "nothing to see here")]
    {:status status
     :headers {"content-type" content-type}
     :body body}))

(def world (game/init))

;; TODO: create a cookie -> state adapter
;; TODO: define views with objects / UI, state to views, etc.!
(defn calculate-move
  [request]
  (let [raw-cookies (or (get (:headers request) "cookie")
                        (get (:headers request) "cookies"))
        cookies (when (string? raw-cookies)
                  (into {}
                        (map (fn [s]
                               (let [[k v] (string/split s #"=")]
                                 [(keyword k) v]))
                             (string/split raw-cookies #"; "))))
        player-id (:player-id cookies)
        player-id (or player-id (rand))
        body (:body request)
        body-size (.capacity body)
        get-body-char (fn [i] (char (.getByte body i)))
        body-text (apply str (map  get-body-char (range body-size)))
        [x y] (cheshire/parse-string body-text)]
    (let [imgdata (game/run world player-id x y)]
      {:status 200
       :headers {"content-type" "application/json"
                 "Set-Cookie" (str "player-id=" player-id)}
       :body (cheshire/generate-string imgdata)})))

(def router
  (atom
   (fn [request]
     (case (:uri request)
       "/" (static request "page.xml" "text/html")
       "/play.js" (static request "play.js" "text/javascript")
       "/move" (calculate-move request)
       ;; this re prevents looking into parent directories
       (let [[full name extension] (re-find #"([^.]+)\.([^.]+)$" (:uri request))
             filename (str name \. extension)
             content-type (get {"txt" "text/plain"
                                "gif" "image/gif"
                                "png" "image/png"
                                "html" "text/html"
                                "jpg" "image/jpeg"}
                               extension
                               "text/plain")]
             (static request filename content-type))))))

(defn clicky-game
  [channel request]
  (lamina/enqueue channel (@router request)))

(defn init
  [& [port]]
  (let [port (try (Integer/parseInt port)
                  (catch Throwable t 8080))]
    (println "starting server on port" port)
    (aleph/start-http-server clicky-game {:port port})
    (nrepl/start-server)))
