(ns j0suetm.teia.component
  "# Components

  Components are Teia's most basic building block.
  
  A component contains the following data:

  - `:name`     An identifiable name

  - `:template` A function that accepts its state and returns the HTML
                template.

  - `:props`    Its inner state.

  - `:components` Its inner components, passed by parameters to minimize
                  external state. This is helpful when testing later on.
                  By having all its data from within, it becomes simpler
                  to manipulate it, beit whatever."
  (:refer-clojure :exclude [compile]))

;; ## Lifetime

;; The Stateful protocol defines the point on when a component has
;; both props and inner components defined.
;;
;; `build` builds a component map, which can be used by `compile` later.
;; Inner components can be accessed and manipulated just like props, but
;; are separated since they need their own state.
;;
;; `compile` compiles component states to itself. Basically `renders`
;; the given component based on its props, after recursively `rendering`
;; its inner components.
(defprotocol Stateful
  (build
    [cmp]
    [cmp props]
    [cmp props components])
  (compile
    [cmp]))

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

(defn $
  "Alias to pipe `build`->`compile` directly. Makes life easier when
  building a component from within another one."
  ([cmp] ($ cmp {} []))
  ([cmp props] ($ cmp props []))
  ([cmp props components]
   (:compiled
    (compile
     (build cmp props components)))))

(defn component?
  [data]
  (instance? Component data))
