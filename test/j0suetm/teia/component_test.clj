(ns j0suetm.teia.component-test
  (:require
   [clojure.test :as t]
   [j0suetm.teia.component :as teia.cmp :refer [$]]))

(t/deftest compile-test
  [(t/testing "props;"
     (t/is (= [:div
               [:p "hello j0suetm"]
               [:ul
                '([:li "clojure"]
                  [:li "go"])]]
              (-> (teia.cmp/->Component
                   :props-test
                   (fn [{:keys [props]}]
                     [:div
                      [:p (str "hello " (:name props))]
                      [:ul (map
                            #(identity [:li %])
                            (:langs props))]]))
                  (teia.cmp/build {:name "j0suetm"
                                   :langs ["clojure" "go"]})
                  (teia.cmp/compile)
                  (:compiled)))))

   (t/testing "compilation failure;"
     (t/is (= [:p "failed to compile component :foobar"]
              (-> (teia.cmp/build
                   (teia.cmp/->Component
                    :foobar
                    (fn [_]
                      (throw (Exception. "something went wrong")))))
                  (teia.cmp/compile)
                  (:compiled)
                  (second)))))

   (t/testing "inner components;"
     (t/is (= [:div [:p "hello teia"]]
              (teia.cmp/$
               (teia.cmp/->Component
                :cmp-1
                (fn [{:keys [components]}]
                  [:div
                   ($ (:hello components)
                      {:name "teia"})]))
               {}
               [(teia.cmp/->Component
                 :hello
                 (fn [{:keys [props]}]
                   [:p (str "hello " (:name props))]))]))))])
