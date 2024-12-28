(ns j0suetm.teia.htmx
  (:require [clj-http.client :as client]))

(defonce ^:private src
  "HTMX's source code."
  (atom nil))

(defn get-src
  "Retrieves HTMX's source code for specific `version`."
  [version]
  (or
   @src
   (let [unpkg-url (str
                    "https://unpkg.com/htmx.org@"
                    version
                    "/dist/htmx.min.js")
         {:keys [status body]} (client/get unpkg-url)]
     (if-not (= status 200)
       ;; TODO: improve logging
       (prn :failure ::get-src :status status :body body)
       (reset! src body)))))

(comment
  (let [a (System/currentTimeMillis)
        _ (get-src "2.0.4")
        b (System/currentTimeMillis)]
    (/ (- b a) 1000))
  ;;
  )
