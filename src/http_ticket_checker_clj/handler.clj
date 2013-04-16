(ns http-ticket-checker-clj.handler
  (:use http-ticket-checker-clj.configuration)
  (:use http-ticket-checker-clj.tickets)
  (:use compojure.core)
  (:use [ring.util.response :only (file-response header content-type)])
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [clojurewerkz.spyglass.client :as m])
  (:require [clojure.data.json :as json]))


(defn init []
  (do
    (set-ticket-store (create-ticket-store))
    (set-config (load-config))))

(defn destroy []
  (do
    (m/shutdown (get-ticket-store))
    (set-ticket-store nil)))


(defn handle-good-ticket [resource]
  (header
    (file-response resource {:root ((get-config) :file_dir)})
    "Cache-Control"
    "no-cache"))

(defn handle-bad-ticket []
  (content-type
    {:status 403
     :body "ticket invalid"}
    "text/plain"))


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
