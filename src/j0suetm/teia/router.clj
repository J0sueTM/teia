(ns j0suetm.teia.router
  "# Routing

  Teia eases the process of routing a component. Besides having this
  as its priority, it still offers support for other purposes, like
  a `application/edn` router."
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

(def component->html-encoder
  "Muuntaja encoder that renders a given component through hiccup.

  See: <https://github.com/metosin/muuntaja/blob/master/modules/muuntaja/src/muuntaja/format/json.clj#L39>"
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

;; ## Route
;;  
;; Teia uses reitit in its back-end. So you can define routes the same
;; way you have been doing with reitit, while also being able to define
;; component routes in-between the other routes. Teia will parse them
;; for you, and re-arrange the entire route tree, with reitit routes
;; wrapping your pre-defined component routes.

(comment
  ;; For example, a sample component route defined like this:
  ;;
  ["/greet/:name"
   {:get
    {:component (teia.cmp/->Component
                 :greeting
                 (fn [{:keys [props]}]
                   [:p (str (:greeting props) ", "
                            (:name props) "!")]))
     :parameters {:path {:name :string}
                  :headers {:greeting :string}}
     :handler (fn [{:keys [path-params headers]}]
                {:status 200
                 :props (merge path-params headers)})}}]
  ;;
  ;; will be adapted to:
  ;;
  ["/greet/:name"
   {:get
    {:parameters {:path {:name :string}
                  :headers {:greeting :string}}
     :handler 'j0suetm.teia.router/component-route-handler
     ;; which renders your component, using the states from
     ;; the handler function.
     }}]
  ;;
  )

(defn component-route-handler
  "Generic reitit handler that uses the return from the defined
  handler as the state to render a route's component."
  [handler component request]
  (try
    ;; The handler's response should be a map containing the props and
    ;; the inner components to be used by a route's component.
    (let [{:keys [status props components]
           :or {status 200
                props {}
                components []}} (handler request)]
      {:status status
       :body (teia.cmp/compile
              (teia.cmp/build component props components))})
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

(defn build
  "Builds a reitit router, adapting component routes to reitit routes
  through the way.

  Options:

  - `:default-format` The response data format. UI router, i.e `text/html`
                      by default."
  [routes & [options]]
  (let [{:keys [default-format]
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

        middlewares (into
                     [reitit.mddwr.ex/exception-middleware
                      reitit.mddwr.muuntaja/format-negotiate-middleware
                      reitit.mddwr.muuntaja/format-request-middleware
                      reitit.mddwr.muuntaja/format-response-middleware
                      reitit.coercion/coerce-request-middleware
                      reitit.mddwr.params/parameters-middleware]
                     ;; No need to verify response when UI router.
                     (when ui-router?
                       [reitit.coercion/coerce-response-middleware]))]
    (reitit/router
     (map #(apply component-route->reitit-route %) routes)
     {:exception reitit.pretty/exception
      :data {:coercion coercion
             :muuntaja muuntaja
             :middleware middlewares}})))
