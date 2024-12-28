(ns core
  (:require
   [j0suetm.teia.core :as teia]
   [j0suetm.teia.router :as teia.router]))

(def todos
  (atom []))

(teia/defcmp todo-list
  [{:keys [props]}]
  [:div
   (if-let [todos (:todos props)]
     [:ul
      (for [todo todos]
        [:li [:p (:content todo)]])]
     [:p "There are no todos. Good job!"])
   [:button "Add TODO"]])

(def router
  (teia.router/build
   [["/todos"
     {:get
      {:component todo-list
       :handler (fn [_]
                  {:status 200
                   :body @todos})}}]]))

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
