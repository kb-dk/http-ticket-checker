(ns http-ticket-checker-clj.handler
  (:use compojure.core)
  (:use [ring.util.response :only (file-response)])
  (:require [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [clojurewerkz.spyglass.client :as m])
  (:require [clojure.data.json :as json]))


(def content_type "Thumbnails")


(def ticket-store
  (m/bin-connection "alhena:11211"))

(defn get-ticket [ticket_id]
  (m/get ticket-store ticket_id))

(defn parse-ticket [raw_ticket]
  (if raw_ticket
    (let [ticket (json/read-str raw_ticket)]
      (let [resource_ids (ticket "resources")
            presentationType (ticket "type")
            userIdentifier (ticket "userIdentifier")]
        (if (and resource_ids type userIdentifier)
          {:resource_ids resource_ids :presentationType presentationType :userIdentifier userIdentifier}
          nil)))
    nil))

(defn get-resource-id [resource]
  (first
    (clojure.string/split
      (last
        (clojure.string/split
          resource
          #"/"))
      #"\.")))

(defn shorten-resource-id [resource_id]
  (last
    (clojure.string/split resource_id #":")))


(defn valid-ticket? [resource ticket_id]
  (let [ticket (get-ticket ticket_id)
        resource_id (get-resource-id resource)]
    (if (and ticket resource_id)
      (let [parsed_ticket (parse-ticket ticket)]
        (if parsed_ticket
          (and
            (= content_type (parsed_ticket :presentationType))
            (not
              (not
                (some #{resource_id} (list
                                       (last
                                         (map shorten-resource-id (parsed_ticket :resource_ids))))))))
          false))
      false)))


(defn handle-good-ticket [resource]
  (file-response resource {:root "/home/adam/src/http-ticket-checker/files"}))

(defn handle-bad-ticket []
  {:status 403, :body "invalid ticket"})


(defroutes app-routes
  (GET "/ticket/:id" [id] (get-ticket id))

;  (GET ["/images/:resource" :resource #"[^?]+"] [resource & params]
;    (str (valid-ticket? resource (params :ticket))))

  (GET ["/images/:resource" :resource #"[^?]+"] [resource & params]
    (if
      (valid-ticket? resource (params :ticket))
      (handle-good-ticket resource)
      (handle-bad-ticket)))

  (route/not-found "Not Found"))


(def app
  (handler/site app-routes))
