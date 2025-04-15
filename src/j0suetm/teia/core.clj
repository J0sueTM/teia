(ns j0suetm.teia.core
  "Teia is a Simple, Fast and Reliable HyperMedia library."
  (:require
   [j0suetm.teia.component :as cmp]
   [hiccup2.core :as hiccup]))

(defmacro defcmp
  "Helper macro that defines a component.

  Simply wraps `j0suetm.teia.component/->Component`."
  [cmp-name args body]
  `(def ~cmp-name
     (j0suetm.teia.component/->Component
      (keyword '~cmp-name)
      (fn ~args
        ~body)
      :component)))

(defmacro defpage
  "Helper macro that defines a page."
  [cmp-name args head body]
  `(def ~cmp-name
    (j0suetm.teia.component/->Component
     (keyword '~cmp-name)
     (fn ~args
       [:html
        {:lang "en"}
        ~(if (not-empty head)
           [:head head]
           [:head])
        [:body ~body]])
     :page)))

(comment
  (defcmp my-cmp
    [{:keys [props components]}]
    [:div
     [:p (:username props)]
     (cmp/$
      (:my-button components)
      {:text "click me"})])

  (defcmp my-button
    [{:keys [props]}]
    [:button (:text props)])

  (cmp/$ my-cmp {:username "j0suetm"} [my-button])
  ;; => [:div [:p "j0suetm"] [:button "click me"]]

  (defpage my-home-page
    [{:keys [components]}]
    []
    (cmp/$
     (:my-cmp components)
     {:username "user123"}
     [my-button]))

  (cmp/$ my-home-page nil [my-cmp])
  )
