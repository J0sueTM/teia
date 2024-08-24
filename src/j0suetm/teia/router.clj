(ns j0suetm.teia.router
  (:require
   [malli.util]
   [muuntaja.core :as muuntaja]
   [reitit.coercion.malli :as reitit.coercion.malli]
   [reitit.dev.pretty :as reitit.pretty]
   [reitit.ring :as reitit]
   [reitit.ring.coercion :as reitit.coercion]
   [reitit.ring.middleware.exception :as reitit.mddwr.ex]
   [reitit.ring.middleware.muuntaja :as reitit.mddwr.muuntaja]
   [reitit.ring.middleware.parameters :as reitit.mddwr.params]))

(defn build
  [routes & options]
  (let [{:keys [default-format]
         :or {default-format "application/edn"}} options]
    (reitit/router
     routes
     {:exception reitit.pretty/exception
      :data {:coercion (reitit.coercion.malli/create
                        {:error-keys #{:type :in :value :errors
                                       :humanized :schema}
                         :compile malli.util/closed-schema
                         :strip-extra-keys true
                         :default-values true
                         :options nil})
             :muuntaja (muuntaja/create
                        (assoc
                         muuntaja/default-options
                         :default-format default-format))
             :middleware [reitit.mddwr.ex/exception-middleware
                          reitit.mddwr.muuntaja/format-negotiate-middleware
                          reitit.mddwr.muuntaja/format-response-middleware
                          reitit.mddwr.muuntaja/format-request-middleware
                          reitit.coercion/coerce-response-middleware
                          reitit.coercion/coerce-request-middleware
                          reitit.mddwr.params/parameters-middleware]}})))
