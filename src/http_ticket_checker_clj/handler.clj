;; ## HTTP requests and -responses, other tidbits in connection with ring.

(ns http-ticket-checker-clj.handler
  (:use [compojure.core])
  (:require [http-ticket-checker-clj.configuration :as config]
            [http-ticket-checker-clj.tickets :as tickets]
            [ring.util.response :as response]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojurewerkz.spyglass.client :as m]))


(defn init
  "Create the connection to memcached, and load the configuration."
  []
  (do
    (config/set-config (config/load-config))
    (tickets/set-ticket-store (tickets/create-ticket-store))))

(defn destroy
  "Shutdown the connection to memcached, and clear the configuration."
  []
  (do
    (m/shutdown (tickets/get-ticket-store))
    (tickets/set-ticket-store nil)))

(def not-found-response
  "Ring reponse used for 404 requests."
  (response/not-found "not found"))

(def forbidden-response
  "Ring response used for 403 requests."
  (response/content-type
    {:status 403
     :body "ticket invalid"}
    "text/plain"))

(defn handle-good-ticket
  "Logic for processing valid tickets."
  [resource]
  (if (config/get-config :use_x_sendfile)
    (response/header {:status 200} "X-Sendfile" (str ((config/get-config) :file_dir) \/ resource))
    (let [response (response/file-response resource {:root ((config/get-config) :file_dir)})]
      (if response
        (response/header
          response
          "Cache-Control"
          "no-cache")
        not-found-response))))

(defn handle-bad-ticket
  "Logic for processing invalid tickets."
  []
  forbidden-response)

(defroutes app-routes
  "Various routes we respond to."
  (GET "/reload" [:as request]
    (if (= (request :remote-addr) "127.0.0.1")
      (do
        (destroy)
        (init)
        {:status 200 :body (pr-str (config/get-config))})
      {:status 403 :body "forbidden"}))

  ; Resources can be basically any path, except paths containing "..".
  ; * The ring request will be mapped to the request-var.
  ; * The resource will be mapped to the resource-var.
  ; * request parameters will end up in the params-map-
  (GET ["/:resource" :resource #"[^?]+"] [:as request resource & params]
    (if
      (and
        (not (re-find #"\.\." resource)) ; the resource should not contain ".."
        (tickets/valid-ticket? resource (params :ticket) (request :remote-addr))) ; remote-addr contains the client ip
      (handle-good-ticket resource)
      (handle-bad-ticket)))

  ; Respond with not-found-response on 404's.
  (route/not-found not-found-response))

(def app
  "Create the application."
  (handler/site app-routes))
