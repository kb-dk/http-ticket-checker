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

  A ticket-store is a map with at least one key:

  * `:get`: 1-ary function used for retrieving a value from the store.

  Optional keys:

  * `:shutdown`: 0-ary function which should shutdown any connections
  to the store.

  Other keys in the map can be used at will, e.g. for storing a
  connection."
  []
  {:connection (m/bin-connection (config/get-config :memcached))
   :get #(m/get ((get-ticket-store) :connection) %1)
   :shutdown #(m/shutdown ((get-ticket-store) :connection))})

(defn shutdown-ticket-store []
  (let [shutdown-fn (:shutdown (get-ticket-store))]
    (if shutdown-fn (shutdown-fn))))

(defn set-ticket-store [new-ticket-store]
  (reset! ticket-store new-ticket-store))


(defn get-ticket
  "Get ticket from memcached, but only if the id is a string with a
  length above 0."
  [raw_ticket_id]
  (let [ticket_id (str raw_ticket_id)]
    (if
      (> (count ticket_id) 0)
      (((get-ticket-store) :get) ticket_id))))

(defn- parse-ticket
  "Parse a ticket from memcached, and return a map with resource ids,
  presentation type and user identifier."
  [raw_ticket]
  (if raw_ticket
    (let [ticket (json/read-str raw_ticket)
          resource_ids (ticket "resources")
          presentationType (ticket "type")
          userIdentifier (ticket "userIdentifier")]
      (if (and resource_ids type userIdentifier)
        {:resource_ids resource_ids
         :presentationType presentationType
         :userIdentifier userIdentifier}))))

(defn- get-resource-id
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

(defn to-boolean
  "Converts anything to a boolean value.

   Examples:

   * true -> true
   * false -> false
   * nil -> false
   * \"horsie\" -> true
   * (_be warned_) {} -> true"

  [value]
  (not (not value)))

(defn valid-ticket?
  "Validate a ticket against the requested resource and user identifier.

   A ticket is considered valid iff

   * the presentation-type from configurations matches the one from the ticket
   * the requested resource is in the list of resources from the ticket
   * the client ip-adresse matches the user identifier from the ticket.

   Example usage:
   <pre><code>(valid-ticket?
     \"/3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg\"
     (get-ticket \"here-be-a-ticket-id\")
     \"127.0.0.1\")</code></pre>"
  [resource ticket user-identifier]
  (let [resource-id (get-resource-id resource)
        parsed-ticket (parse-ticket ticket)]
    (and
      (to-boolean resource-id)
      (to-boolean parsed-ticket)
      (not (re-find #"\.\." resource)) ; the resource should not contain ".."
      (= ((config/get-config) :presentation_type) (parsed-ticket :presentationType))
      (= user-identifier (parsed-ticket :userIdentifier))
      (to-boolean
        (some #{resource-id}
          (map shorten-resource-id (parsed-ticket :resource_ids)))))))
