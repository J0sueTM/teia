(ns j0suetm.teia.router-test
  (:require
   [clojure.test :as t]
   [j0suetm.teia.component :as teia.cmp]
   [j0suetm.teia.router :as teia.router]
   [muuntaja.core :as muuntaja]
   [reitit.ring :as reitit]))
(t/deftest component-route-handler-test
  (t/is (= [:p "hello teia"]
           (:compiled
            (:body
             (teia.router/component-route-handler
              (fn [{:keys [path-params]}]
                {:status 200
                 :body (select-keys path-params [:name])})
              (teia.cmp/map->Component
               {:name :says-hello
                :template (fn [{:keys [props]}]
                            [:p (str "hello " (:name props))])})
              {:path-params {:name "teia"}}))))))

(t/deftest component-route->reitit-route-test
  [(t/testing "single route;"
     (t/is (= '(nil nil)
              (->> ["/foo/bar"
                    {:get {:component (teia.cmp/->Component
                                       :empty [:div])
                           :handler (fn [_] {})}
                     :post {:component (teia.cmp/->Component
                                        :empty [:div])
                            :handler (fn [_] {})}}]
                   (apply teia.router/component-route->reitit-route)
                   (second)
                   (vals)
                   (map :component)))))
   (t/testing "multiple routes;"
     (let [route (->> ["/foo"
                       {:get {:component (teia.cmp/->Component
                                          :empty [:div])
                              :handler (fn [_] {})}}
                       ["/bar"
                        {:get {:component (teia.cmp/->Component
                                           :empty [:div])
                               :handler (fn [_] {})}}]]
                      (apply teia.router/component-route->reitit-route))]
       [(t/is (not (get-in route [2 :get :component])))
        (t/is (not (get-in route [3 2 :get :component])))]))])

(t/deftest build-test
  [(t/testing "supported formats;"
     (let [routes [["/users/:name"
                    {:get
                     {:parameters {:path {:name :string}}
                      :responses {200 [:map [:username :string]]}
                      :handler (fn [{:keys [path-params]}]
                                 (let [{:keys [name]} path-params]
                                   {:status 200
                                    :body {:username name}}))}}]
                   ["/users/:name/plain-html"
                    {:get
                     {:parameters {:path {:name :string}}
                      :handler (fn [{:keys [path-params]}]
                                 (let [{:keys [name]} path-params]
                                   {:status 200
                                    :body [:p "hello " name]}))}}]]]
       [(t/testing "html;"
          (t/is (= "<p>hello teia</p>"
                   (->> ((reitit/ring-handler
                          (teia.router/build routes))
                         {:request-method :get
                          :uri "/users/teia/plain-html"})
                        (:body)
                        (slurp)))))
        (t/testing "edn;"
          (t/is (= {:username "j0suetm"}
                   (->> ((reitit/ring-handler
                          (teia.router/build
                           routes
                           {:default-format "application/edn"}))
                         {:request-method :get
                          :uri "/users/j0suetm"})
                        (:body)
                        (muuntaja/decode "application/edn")))))
        (t/testing "json;"
          (t/is (= {:username "j0suetm"}
                   (->> ((reitit/ring-handler
                          (teia.router/build
                           routes
                           {:default-format "application/json"}))
                         {:request-method :get
                          :uri "/users/j0suetm"})
                        (:body)
                        (muuntaja/decode "application/json")))))]))

   (t/testing "component route;"
     (let [routes [["/greet/:name"
                    {:get
                     {:component (teia.cmp/->Component
                                  :greeting
                                  (fn [{:keys [props]}]
                                    [:p (str (:greeting props)
                                             ", "
                                             (:name props)
                                             "!")]))
                      :parameters {:path {:name string?}
                                   :headers {:greeting string?}}
                      :handler (fn [{:keys [path-params headers]}]
                                 {:status 200
                                  :body (merge path-params headers)})}
                     :post {:component (teia.cmp/->Component
                                        :failing
                                        (fn [_]
                                          (throw
                                           (Exception.
                                            "should fail"))))
                            :handler (fn [_]
                                       {:status 200
                                        :body {}})}}]]]
       [(t/is (= "<p>hope all is well, darling!</p>"
                 (->> ((reitit/ring-handler
                        (teia.router/build routes))
                       {:request-method :get
                        :uri "/greet/darling"
                        :headers {:greeting "hope all is well"}})
                      (:body)
                      (slurp))))
        (t/is (->> ((reitit/ring-handler
                     (teia.router/build routes))
                    {:request-method :post
                     :uri "/greet/failing"})
                   (:body)
                   (slurp)
                   (re-find
                    #"failed to compile component :failing")))]))])
