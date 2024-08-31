(ns j0suetm.teia.router-test
  (:require
   [clojure.test :as t]
   [j0suetm.teia.router :as teia.router]
   [muuntaja.core :as muuntaja]
   [reitit.ring :as reitit]))

(t/deftest build-test
  (let [routes [["/users/:name"
                 {:get
                  {:parameters {:path {:name :string}}
                   :responses {200 [:map [:username :string]]}
                   :handler (fn [{:keys [path-params]}]
                              (let [{:keys [name]} path-params]
                                {:status 200
                                 :body {:username name}}))}}]
                ["/users/:name/ui"
                 {:get
                  {:parameters {:path {:name :string}}
                   :handler (fn [{:keys [path-params]}]
                              (let [{:keys [name]} path-params]
                                {:status 200
                                 :headers {:content-type "text/html"}
                                 :body [:p "hello " name]}))}}]]]
    (t/testing "supported formats;"
      (t/testing "html;"
        (t/is (= "<p>hello j0suetm</p>"
                 (->> ((reitit/ring-handler
                        (teia.router/build routes))
                       {:request-method :get
                        :uri "/users/j0suetm/ui"})
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
                      (muuntaja/decode "application/json"))))))))
