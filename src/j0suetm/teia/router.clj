(ns j0suetm.teia.router
  (:require
   [hiccup2.core :as hiccup]
   [malli.util]
   [muuntaja.core :as muuntaja]
   [muuntaja.format.core :as muuntaja.fmt]
   [reitit.coercion.malli :as reitit.coercion.malli]
   [reitit.dev.pretty :as reitit.pretty]
   [reitit.ring :as reitit]
   [reitit.ring.coercion :as reitit.coercion]
   [reitit.ring.middleware.exception :as reitit.mddwr.ex]
   [reitit.ring.middleware.muuntaja :as reitit.mddwr.muuntaja]
   [reitit.ring.middleware.parameters :as reitit.mddwr.params])
  (:import
   [java.io OutputStream]))

(def html-format
  "Muuntaja format to for text/html"
  (let [enc-fn (fn [_]
                 (reify
                   ;;
                   muuntaja.fmt/EncodeToBytes
                   (encode-to-bytes [_ data charset]
                     (.getBytes
                      (str (hiccup/html data))
                      ^String charset))
                   ;;
                   muuntaja.fmt/EncodeToOutputStream
                   (encode-to-output-stream [_ data charset]
                     (fn [^OutputStream output-stream]
                       (.write
                        output-stream
                        (.getBytes
                         (str (hiccup/html data))
                         ^String charset))))))]
    (muuntaja.fmt/map->Format
     {:name "text/html"
      :encoder [enc-fn]})))

(defn build
  "Builds a basic ring router."
  [routes & [options]]
  (let [{:keys [default-format]
         ;; assumes UI router by default
         :or {default-format "text/html"}} options

        ui-router? (= default-format "text/html")

        coercion (reitit.coercion.malli/create
                  {:error-keys #{:type :in :value :errors
                                 :humanized :schema}
                   :compile malli.util/closed-schema
                   :strip-extra-keys true
                   :default-values true
                   :options nil})

        muuntaja (muuntaja/create
                  (-> (assoc-in
                       muuntaja/default-options
                       [:formats "text/html"]
                       html-format)
                      (assoc :default-format default-format)))

        ;; doesn't need to verify response when UI router
        middlewares (into
                     [reitit.mddwr.ex/exception-middleware
                      reitit.mddwr.muuntaja/format-negotiate-middleware
                      reitit.mddwr.muuntaja/format-request-middleware
                      reitit.mddwr.muuntaja/format-response-middleware
                      reitit.coercion/coerce-request-middleware
                      reitit.mddwr.params/parameters-middleware]
                     (when ui-router?
                       [reitit.coercion/coerce-response-middleware]))]
    (reitit/router
     routes
     {:exception reitit.pretty/exception
      :data {:coercion coercion
             :muuntaja muuntaja
             :middleware middlewares}})))
