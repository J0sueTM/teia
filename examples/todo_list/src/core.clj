(ns core
  (:require
   [j0suetm.teia.core :as teia]
   [j0suetm.teia.component :as teia.cmp]
   [j0suetm.teia.router :as teia.router]))

(defonce todos
  (atom nil))

(teia/defcmp todo-list
  [{:keys [props]}]
  [:div
   (if-let [todos (:todos props)]
     [:ul
      (for [todo todos]
        [:li [:p (:content todo)]])]
     [:p "There are no todos. Good job!"])
   [:button "Add TODO"]])

(teia/defpage home-page
  [{:keys [components]}]
  []
  [:div
   [:p "Teia's TODO App"]
   (teia.cmp/$ (:todo-list components))])

(def router
  (teia.router/build
   [["/todos"
     {:get
      {:component todo-list
       :handler (fn [_]
                  {:status 200
                   :props {:todos @todos}})}}]
    ["/"
     {:get
      {:component home-page
       :handler (fn [_] {:status 200})}}]]))

(defn -main
  [& _]
  (let [srv (teia.router/serve! router {:port 7314 :join? false})]
    (.addShutdownHook
     Runtime/getRuntime
     (Thread. ^Runnable #(.stop srv)))))

(comment
  (-main)

  (def srv
    (teia.router/serve! router {:port 7314 :join? false}))
 
  (.stop srv)
  ;;
  )
