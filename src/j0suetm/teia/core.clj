(ns j0suetm.teia.core
  "Teia is a Simple, Fast and Reliable HyperMedia library."
  (:require [j0suetm.teia.component :as cmp]))

(defmacro defcmp
  "Helper macro that defines a component.

  Simply wraps `j0suetm.teia.component/->Component`."
  [cmp-name args body]
  `(def ~cmp-name
     (j0suetm.teia.component/->Component
      (keyword '~cmp-name)
      (fn ~args
        ~body))))

(comment
  (defcmp my-cmp
    [{:keys [props components]}]
    [:div
     [:p (:username props)]
     (cmp/$ (:my-button components)
            {:text "click me"})])

  (defcmp my-button
    [{:keys [props]}]
    [:button (:text props)])

  (cmp/$ my-cmp {:username "j0suetm"} [my-button])
  ;; => [:div [:p "j0suetm"] [:button "click me"]]
  ;;
  )
