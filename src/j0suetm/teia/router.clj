(ns j0suetm.teia.router
  (:require
   [hiccup2.core :as hiccup]
   [j0suetm.teia.component :as teia.cmp]
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

(defn component-route-handler
  [handler component request]
  (try
    ;; The handler's response should be a map, which will be used
    ;; as the props to build the component.
    {:status 200
     :body (->> (handler request)
                (:body)
                (teia.cmp/build component)
                (teia.cmp/compile))}
    (catch Exception e
      {:status 500
       :body (teia.cmp/build
              (teia.cmp/map->Component teia.cmp/failure-cmp)
              {:reason "failed to handle component route"
               :exception e})})))

(defn component-route->reitit-route
  "Adapts a component route to a reitit router.

  Recursively applies itself to any sibling route."
  [uri methods & sibling-routes]
  (into
   [uri (reduce-kv
         (fn [methods method
              {:keys [handler component]
               :as definition}]
           (let [handler' (partial component-route-handler
                                   handler component)]
             (assoc methods method
                    (if component
                      (-> (assoc definition :handler handler')
                          (dissoc :component))
                      definition))))
         {} methods)]
   (mapv
    (partial apply component-route->reitit-route)
    sibling-routes)))

(def component->html-encoder
  (reify
    muuntaja.fmt/EncodeToBytes
    (encode-to-bytes [_ data charset]
      (-> (if (teia.cmp/component? data)
            (:compiled (teia.cmp/compile data))
            data)
          (hiccup/html)
          (str)
          (.getBytes ^String charset)))

    muuntaja.fmt/EncodeToOutputStream
    (encode-to-output-stream [_ data charset]
      (fn [^OutputStream output-stream]
        (.write
         output-stream
         (-> (if (teia.cmp/component? data)
               (:compiled (teia.cmp/compile data))
               data)
             (hiccup/html)
             (str)
             (.getBytes ^String charset)))))))

(def html-format
  "Muuntaja format for text/html"
  (let [enc-fn (fn [_]
                 component->html-encoder)]
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
     (map #(apply component-route->reitit-route %) routes)
     {:exception reitit.pretty/exception
      :data {:coercion coercion
             :muuntaja muuntaja
             :middleware middlewares}})))
