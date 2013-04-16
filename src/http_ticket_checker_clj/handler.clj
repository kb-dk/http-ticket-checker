(ns http-ticket-checker-clj.handler
  (:use http-ticket-checker-clj.configuration)
  (:use http-ticket-checker-clj.tickets)
  (:use compojure.core)
  (:require [ring.util.response :as response]
            [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [clojurewerkz.spyglass.client :as m]))


(defn init []
  (do
    (set-ticket-store (create-ticket-store))
    (set-config (load-config))))

(defn destroy []
  (do
    (m/shutdown (get-ticket-store))
    (set-ticket-store nil)))

(def not-found-response
  (response/not-found "not found"))

(def forbidden-response
  (response/content-type
    {:status 403
     :body "ticket invalid"}
    "text/plain"))


(defn handle-good-ticket [resource]
  (let [response (response/file-response resource {:root ((get-config) :file_dir)})]
    (if response
      (response/header
        response
        "Cache-Control"
        "no-cache")
      not-found-response)))

(defn handle-bad-ticket []
  forbidden-response)


(defroutes app-routes
  (GET "/ticket/:id" [id] (get-ticket id))
  (GET "/reconnect" [] (str (init)))

  (GET ["/:resource" :resource #"[^?]+"] [resource & params]
    (if (re-find #"\.\." resource)
      (handle-bad-ticket)
      (if
        (valid-ticket? resource (params :ticket))
        (handle-good-ticket resource)
        (handle-bad-ticket))))

  (route/not-found "Not Found"))


(def app
  (handler/site app-routes))
