(ns j0suetm.teia.component
  "Teia's base concept."
  (:refer-clojure :exclude [compile]))

(defprotocol Stateful
  (build
    [cmp]
    [cmp props]
    [cmp props components]
    "Builds a component map, which can be used by `compile` later.

Inner components can be accessed and manipulated just like props, but
are separated since they need their own state.")
  (compile
    [cmp]
    "Compiles component states to itself. Basically 'renders' the
given component based on its props, after recursively 'rendering' its
inner components."))

(def compilation-failure-cmp
  "Barebones component for component compilation failure.

  Defined like this because the Component record isn't defined yet."
  {:name :compilation-failure
   :template (fn [{:keys [props]}]
               [:div
                [:p (str "failed to compile component "
                         (:component/name props))]
                (let [ex (:exception props)
                      stacktrace (->> ex
                                      (.getStackTrace)
                                      (map str))]
                  [:ul
                   [:li [:p (.getMessage ex)]]
                   (for [trace stacktrace]
                     [:li [:p trace]])])])})

(defrecord Component [name template]
  Stateful
  (build [cmp]
    (build cmp {} []))
  (build [cmp props]
    (build cmp props []))
  (build [cmp props components]
    (merge cmp {:props props
                :components components}))
  (compile [cmp]
    (let [{:keys [name template props components]} cmp]
      (try
        (assoc
         cmp :compiled
         (template
          {:props props
           ;; Inner components come as a list. Here we reduce them to
           ;; a map in order for easier retrieval when applying the
           ;; parent component
           :components (reduce
                        (fn [cmp-map cmp]
                          (assoc
                           cmp-map
                           (:name cmp)
                           (if-let [?compiled (:compiled cmp)]
                             ?compiled
                             (:compiled (compile cmp)))))
                        {} components)}))
        (catch Exception e
          ;; TODO: provide better ways to handle exceptions.
          ;; Enable the end user to control it as well.
          (compile
           (build
            (map->Component compilation-failure-cmp)
            {:exception e
             :component/name name})))))))

(defmacro defcmp
  "Helper macro that defines a component.

  Simply wraps `j0suetm.teia.component/->Component`."
  [cmp-name args body]
  `(j0suetm.teia.component/->Component
    (keyword '~cmp-name)
    (fn ~args
      ~@body)))
