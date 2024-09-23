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

(def failure-cmp
  "Barebones component for failure showcase.

  Defined like this because the Component record isn't defined yet."
  {:name :failure
   :template (fn [{:keys [props]}]
               [:div
                [:p (:reason props)]
                (let [ex (:exception props)
                      stacktrace (map str (.getStackTrace ex))]
                  [:ul
                   [:li [:p (.getMessage ex)]]
                   (for [trace stacktrace]
                     [:li [:p trace]])])])})

(defrecord Component [name template]
  Stateful
  (build
    [cmp]
    (build cmp {} []))
  (build
    [cmp props]
    (build cmp props []))
  (build
    [cmp props components]
    (merge cmp {:props props
                :components components}))
  (compile
    [cmp]
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
                           (or (:compiled cmp) cmp)))
                        {} components)}))
        (catch Exception e
          ;; TODO: provide better ways to handle exceptions.
          ;; Enable the end user to control it as well.
          (compile
           (build
            (map->Component failure-cmp)
            {:reason (str "failed to compile component " name)
             :exception e})))))))

;; Alias to pipe `build`->`compile` directly. Makes life easier when
;; building a component from within another one.
(defn $
  ([cmp] ($ cmp {} []))
  ([cmp props] ($ cmp props []))
  ([cmp props components]
   (:compiled
    (compile
     (build cmp props components)))))

(defn component?
  [data]
  (and (:name data) (:template data)))
