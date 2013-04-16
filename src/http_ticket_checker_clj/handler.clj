(ns http-ticket-checker-clj.handler
  (:use [compojure.core])
  (:require [http-ticket-checker-clj.configuration :as config]
            [http-ticket-checker-clj.tickets :as tickets]
            [ring.util.response :as response]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojurewerkz.spyglass.client :as m]))


(defn init []
  (do
    (tickets/set-ticket-store (tickets/create-ticket-store))
    (config/set-config (config/load-config))))

(defn destroy []
  (do
    (m/shutdown (tickets/get-ticket-store))
    (tickets/set-ticket-store nil)))

(def not-found-response
  (response/not-found "not found"))

(def forbidden-response
  (response/content-type
    {:status 403
     :body "ticket invalid"}
    "text/plain"))

(defn handle-good-ticket [resource]
  (let [response (response/file-response resource {:root ((config/get-config) :file_dir)})]
    (if response
      (response/header
        response
        "Cache-Control"
        "no-cache")
      not-found-response)))

(defn handle-bad-ticket []
  forbidden-response)


(defroutes app-routes
  (GET "/ticket/:id" [id] (tickets/get-ticket id))
  (GET "/reconnect" [] (str (init)))

  (GET ["/:resource" :resource #"[^?]+"] [:as request resource & params]
    (if (re-find #"\.\." resource)
      (handle-bad-ticket)
      (if
        (tickets/valid-ticket? resource (params :ticket) (request :remote-addr))
        (handle-good-ticket resource)
        (handle-bad-ticket))))

  (route/not-found "Not Found"))


(def app
  (handler/site app-routes))
