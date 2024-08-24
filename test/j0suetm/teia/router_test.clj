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
                   :handler (fn [{:keys [headers path-params]}]
                              (let [{:keys [name]} path-params]
                                {:status 200
                                 :headers (when-let [accept (:accept headers)]
                                            {:content-type accept})
                                 :body {:username name}}))}}]]]
    (t/testing "supported formats;"
      (t/testing "default format;"
        (t/is (= {:username "j0suetm"}
                 (->> ((reitit/ring-handler (teia.router/build routes))
                       {:request-method :get
                        :uri "/users/j0suetm"})
                      (:body)
                      (muuntaja/decode "application/edn")))))
      (t/testing "others (json only, for simplicity);"
        (t/is (= {:username "j0suetm"}
                 (->> ((reitit/ring-handler
                        (teia.router/build
                         routes
                         {:default-format "application/json"}))
                       {:request-method :get
                        :uri "/users/j0suetm"})
                      (:body)
                      (muuntaja/decode "application/json"))))))
    (t/testing "html format;")))
