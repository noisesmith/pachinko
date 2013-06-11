(ns noisesmith.pachinko.dataflow
  (:require [clojure.set :refer [union]]))

(defn connect
  "return a function that alters the connections so reciever gets message"
  [message receiver]
  (fn [connections] (update-in connections [message] conj receiver)))

(defn disconnect
  "return a function that alters the world so the receiver does not listen to
   message"
  [message reciever]
  (fn [connections] (update-in connections [message]
                               (partial remove #{reciever}))))

(defn fact
  "returns a function that adds a constant fact to facts"
  [receiver value]
  (fn [facts] (assoc facts receiver value)))

(defn forget
  "returns a function that adds a constant fact to facts"
  [receiver]
  (fn [facts] (dissoc facts receiver)))

(defn update-state
  "updates state in some part of a world"
  [[message params :as input] lookup action state debug]
  (when (lookup message)
    (when debug
      (println update-state "\nmessage" message)
      (println "enter state" state))
    (doseq [modification (partition 2 params)]
      (when debug (println "updating" modification))
      (swap! state (apply action modification)))
    (when debug (println "exit state" state \newline)))
  input)

(defn reduce-actions
  [message parameters facts-atom]
  (fn [result action]
    (if-let [response (and action (action message parameters @facts-atom))]
      (concat response result)
      result)))

(def connect-message? #{::connect ::>>})
(def disconnect-message? #{::disconnect ::<!>})
(def fact-message? #{::remember ::+})
(def forget-message? #{::forget ::-})
(def meta-message?
  (union connect-message? disconnect-message? fact-message? forget-message?))

(defn run
  "keep running a world until there are no more answers to messages
   connections are the routing of messages,
   nodes are the binding of receivers to their implementation,
   facts are cold inputs, thing that do not trigger new output"
  [[connections-atom facts-atom :as world]
   inputs]
  (doseq [[message parameters :as input] (partition 2 inputs)]
    ;; conduct the meta level operations first
    (let [debug (get @facts-atom :debug)]
      (-> input
          (update-state connect-message? connect connections-atom debug)
          (update-state disconnect-message? disconnect connections-atom debug)
          (update-state fact-message? fact facts-atom debug)
          (update-state forget-message? forget facts-atom debug))
      (let [actions (get @connections-atom message)
            _ (when debug
                (println run "message" message "parameters" parameters
                         "actions" actions)
                (when (and (empty? actions) (not (meta-message? message)))
                  (println "NOBODY RESPONDS TO" message)))
            reducer (reduce-actions message parameters facts-atom)
            result (reduce reducer [] actions)]
        (let [messages (filter first (partition 2 result))
              new-args (apply concat result)]
          (when messages (run world new-args))))))
  world)

(defn new-world
  []
  ;; connections / facts
  [(atom {}) (atom {})])

(defn debug-world
  []
  (let [[c f] (new-world)]
    (swap! f assoc :debug true)
    [c f]))

(defn answer
  [[connections facts :as world] message]
  (get @facts message))

(defn delimit
  [el seq]
  (let [parted (partition-by #{el} seq)]
    (case (count parted)
      0 [[][]]
      1 [[] (first parted)]
      2 [[] (nth parted 1)]
      3 [(vec (nth parted 0)) (nth parted 2)])))

(defn generate->>-body
  [input]
  (let [[params input] [(first input) (rest input)]
        [empty input] (delimit '-| input)
        [binding input] (delimit '|- input)
        [empty' input] (delimit '-= input)
        [intermediate input] (delimit '=- input)
        [empty'' input] (delimit '-_ input)
        [actions input] (delimit '_- input)
        [empty''' messages] (delimit '... input)]
    [:params params
     :empty empty
     :bind binding
     :empty' empty'
     :intermediate intermediate
     :empty'' empty''
     :actions actions
     :empty''' empty'''
     :messages (mapv vec (partition 2 messages))]))

(defmacro >>*
  [params bind intermediate actions messages]
    `(fn [~@params {:keys ~bind}]
       (let ~intermediate
         ~@actions
         ~messages)))

(defmacro >>
  [& input]
  (let [in (->> input generate->>-body (partition 2) (map vec) (into {}))]
    `(>>* ~(:params in) ~(:bind in) ~(:intermediate in) ~(:actions in)
          ~(:messages in))))
