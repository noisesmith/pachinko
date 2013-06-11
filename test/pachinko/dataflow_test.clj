(ns pachinko.dataflow-test
  (:require [clojure.test :as test :refer :all]
            [pachinko.dataflow :as >]))

(deftest dataflow
  ;; bang triggers a
  ;; a invokes action-a
  ;; action-a connects :calculate to answer
  ;; action-a sends a :calculate message
  ;; answer establishes :result as a fact with value of 42
  ;; >/answer :result gathers that output
  (testing "remember / connect"
    (let [answer (>/>> [m p] ...
                       ::>/remember [::result 42])
          action-a (>/>> [m p] ...
                         ::>/connect [::calculate answer]
                         ::calculate [])]
      (is (= 42
             (-> (>/new-world)
                 (>/run [::>/connect [::bang action-a]])
                 (>/run [::bang []])
                 (>/answer ::result))))))
  (testing "+ / >>"
    (let [answer (>/>> [_ _] ... ::>/+ [::result 42])
          action-a (>/>> [_ message] ... message _)
          action-b (>/>> [_ _] ... ::>/>> [::calculate answer]
                                   ::calculate [])]
      (is (= 42
             (-> (>/new-world)
                 (>/run [::>/>> [::bang action-a
                                 ::next action-b]])
                 (>/run [::bang ::next])
                 (>/answer ::result))))))
  (testing "<!>"
    (let [world (>/new-world)
          append (>/>>
                  [_ _]
                  -| acc |-
                  ...
                  ::>/+ [:acc (conj acc :a)])]
      (>/run world [::>/>> [:! append] :! nil])
      (is (= [:a] (>/answer world :acc)))
      (>/run world [:! nil])
      (is (= [:a :a] (>/answer world :acc)))
      (>/run world [::>/<!> [:! append] :! nil])
      (is (= [:a :a] (>/answer world :acc))))))

(defn unauto
  "allows us to unit test the expansion of macros"
  [item]
  (cond
   (and (coll? item) (empty? item)) item
   (coll? item) (concat [(unauto (first item))] (unauto (rest item)))
   (symbol? item) (symbol (re-find #"[^_]+" (str item)))
   :default item))

(let [input '([message params]
                -| x y |-
                -= x 0 =-
                -_ (println :ok) (+ params x) _-
                ...
                :continue true
                :status :ok)
      body (>/generate->>-body input)
      result (into {} (map vec (partition 2 body)))]
  (deftest delimit
    (is (= ['[[message params]]
            '[x y |-
              -= x 0 =-
              -_ (println :ok) (+ params x) _-
              ...
              :continue true
              :status :ok]]
           (>/delimit '-| input))))
  (deftest generate->>-body
      (is (= '[message params] (:params result)))
      (is (= [] (:empty result)))
      (is (= '[x y] (:bind result)))
      (is (= [] (:empty' result)))
      (is (= '[x 0] (:intermediate result)))
      (is (= [] (:empty'' result)))
      (is (= '[(println :ok) (+ params x)] (:actions result)))
      (is (= [] (:empty''' result)))
      (is (= [[:continue true] [:status :ok]] (:messages result)))
      (is (= [:params '[message params]
              :empty []
              :bind '[x y]
              :empty' []
              :intermediate '[x 0]
              :empty'' []
              :actions '[(println :ok) (+ params x)]
              :empty''' []
              :messages [[:continue true] [:status :ok]]]
             body)))
  (deftest >>*
    (let [params (:params result)
          bind (:bind result)
          intermediate (:intermediate result)
          actions (:actions result)
          messages (:messages result)]
      (is (= '(clojure.core/fn [message params {:keys [x y]}]
                               (clojure.core/let [x 0]
                                                 (println :ok)
                                                 (+ params x)
                                                 [[:continue true]
                                                  [:status :ok]]))
             (macroexpand-1 `(>/>>* ~params
                                     ~bind
                                     ~intermediate
                                     ~actions
                                     ~messages))))
      (is (fn? (>/>>* [message params]
                       [x y]
                       [x 0]
                       [(println :ok) (+ params x)]
                       [[:continue true] [:status :ok]])))))
  (deftest >>
    ;; tests a macro that no longer exists
    #_(is (= '(clojure.core/let
               [in (clojure.core/->> input
                                     func-comp.dataflow/generate->>-body
                                     (clojure.core/partition 2)
                                     (clojure.core/map clojure.core/vec)
                                     (clojure.core/into {}))
                params (:params in)
                bind (:bind in)
                intermediate (:intermediate in)
                actions (:actions in)
                messages (:messages in)]
               (func-comp.dataflow/>>*
                params bind intermediate actions messages))
             (unauto (macroexpand-1 '(>/>>-proxy input)))))
    (let [test-atom (atom nil)
          nodey (>/>> [message params]
                    -| x y |-
                    -= z 1 =-
                    -_ (println :ok) (reset! test-atom (+ params x z)) _-
                    ...
                    :continue true
                    :status :ok)
          call (fn [] (nodey nil 7 {:x 3}))]
      (is (fn? nodey))
      (is (= [[:continue true] [:status :ok]] (call)))
      (is (= 11 @test-atom))
      (is (= ":ok\n" (with-out-str (call)))))))
