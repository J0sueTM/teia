(ns core
  (:require
   [j0suetm.teia.core :as teia]
   [j0suetm.teia.router :as teia.router]))

(teia/defcmp todo-list
  [_]
  [:ul
   [:li "todo 1"]
   [:li "todo 2"]])

(def router
  (teia.router/build
   [["/todos"
     {:get
      {:component todo-list
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
