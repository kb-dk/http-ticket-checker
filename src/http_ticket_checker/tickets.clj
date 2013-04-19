;; ## Retrieval and validation of tickets.

(ns http-ticket-checker.tickets
  (:require [http-ticket-checker.configuration :as config]
            [clojurewerkz.spyglass.client :as m]
            [clojure.data.json :as json]))


(def ticket-store
  "Atom which holds the connection to the ticket store."
  (atom nil))

(defn get-ticket-store []
  (deref ticket-store))

(defn create-ticket-store
  "Create a ticket-store pointing to the memcached host specified
  in the configuration file.

  A ticket-store is a map with at least one value called `:get`, which
  should be a 1-ary function that recieves a value from the store.
  Other keys in the map can be used at will, e.g. for storing a
  connection."
  []
  {:get #(m/get ((get-ticket-store) :connection) %1)
   :connection (m/bin-connection (config/get-config :memcached))})

(defn set-ticket-store [new-ticket-store]
  (swap! ticket-store
    (fn [_] new-ticket-store)))


(defn get-ticket
  "Get ticket from memcached, but only if the id is a string with a
  length above 0."
  [raw_ticket_id]
  (let [ticket_id (str raw_ticket_id)]
    (if
      (> (count ticket_id) 0)
      (((get-ticket-store) :get) ticket_id)
      nil)))

(defn parse-ticket
  "Parse a ticket from memcached, and return a map with resource ids,
  presentation type and user identifier."
  [raw_ticket]
  (if raw_ticket
    (let [ticket (json/read-str raw_ticket)]
      (let [resource_ids (ticket "resources")
            presentationType (ticket "type")
            userIdentifier (ticket "userIdentifier")]
        (if (and resource_ids type userIdentifier)
          {:resource_ids resource_ids :presentationType presentationType :userIdentifier userIdentifier}
          nil)))
    nil))

(defn get-resource-id
  "Get the resource id from the quested file.
   The id is defined as the substring starting
   after the first `/` and ending before the first `.`.

   E.g. `a/b/c/d/resource-id-here.something`."
  [resource]
  (first
    (clojure.string/split
      (last
        (clojure.string/split
          resource
          #"/"))
      #"\.")))

(defn shorten-resource-id
  "Given a list of resource-ids with 'stuff' in front of the uuid,
   return only the uuid-part.

   E.g. uuid:abcd -> abcd."
  [resource_id]
  (last
    (clojure.string/split resource_id #":")))

(defn valid-ticket?
  "Validate a ticket against the requested resource and user identifier.

   A ticket is considered valid iff

   * the presentation-type from configurations matches the one from the ticket
   * the requested resource is in the list of resources from the ticket
   * the client ip-adresse matches the user identifier from the ticket."
  [resource ticket user-identifier]
  (let [resource_id (get-resource-id resource)]
    (if (and ticket resource_id)
      (let [parsed_ticket (parse-ticket ticket)]
        (if parsed_ticket
          (and
            (= ((config/get-config) :presentation_type) (parsed_ticket :presentationType))
            (= user-identifier (parsed_ticket :userIdentifier))
            (not
              (not
                (some #{resource_id} (list
                                       (last
                                         (map shorten-resource-id (parsed_ticket :resource_ids))))))))
          false))
      false)))
