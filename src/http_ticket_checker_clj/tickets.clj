(ns http-ticket-checker-clj.tickets
  (:use http-ticket-checker-clj.configuration)
  (:require [clojurewerkz.spyglass.client :as m]
            [clojure.data.json :as json]))


(def ticket-store-atom
  (atom nil))

(defn create-ticket-store []
  (m/bin-connection "alhena:11211"))

(defn get-ticket-store []
  (deref ticket-store-atom))

(defn set-ticket-store [ticket-store]
  (swap! ticket-store-atom
    (fn [_] ticket-store)))


(defn get-ticket [raw_ticket_id]
  (let [ticket_id (str raw_ticket_id)]
    (if
      (> (count ticket_id) 0)
      (m/get (get-ticket-store) ticket_id)
      nil)))

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


(defn valid-ticket? [resource ticket_id user-identifier]
  (let [ticket (get-ticket ticket_id)
        resource_id (get-resource-id resource)]
    (if (and ticket resource_id)
      (let [parsed_ticket (parse-ticket ticket)]
        (if parsed_ticket
          (and
            (= ((get-config) :presentation_type) (parsed_ticket :presentationType))
            (= user-identifier (parsed_ticket :userIdentifier))
            (not
              (not
                (some #{resource_id} (list
                                       (last
                                         (map shorten-resource-id (parsed_ticket :resource_ids))))))))
          false))
      false)))
