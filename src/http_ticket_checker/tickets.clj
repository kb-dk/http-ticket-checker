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
  [raw-ticket-id]
  (let [ticket-id (str raw-ticket-id)]
    (if
      (> (count ticket-id) 0)
      (((get-ticket-store) :get) ticket-id))))

(defn- parse-ticket
  "Parse a ticket from memcached, and return a map with resource ids,
  presentation type and user ip address."
  [raw-ticket]
  (if raw-ticket
    (let [ticket (json/read-str raw-ticket)
          resource-ids (ticket "resources")
          presentation-type (ticket "type")
          ip-address (ticket "ipAddress")]
      (if (and resource-ids type ip-address)
        {:resource-ids resource-ids
         :presentation-type presentation-type
         :ip-address ip-address}))))

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
  [resource-id]
  (last
    (clojure.string/split resource-id #":")))

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
  "Validate a ticket against the requested resource and user ip address.

   A ticket is considered valid iff

   * the presentation-type from configurations matches the one from the ticket
   * the requested resource is in the list of resources from the ticket
   * the client ip-adresse matches the user ip address from the ticket.

   Example usage:
   <pre><code>(valid-ticket?
     \"/3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg\"
     (get-ticket \"here-be-a-ticket-id\")
     \"127.0.0.1\")</code></pre>"
  [resource ticket ip-address]
  (let [resource-id (get-resource-id resource)
        parsed-ticket (parse-ticket ticket)]
    (and
      (to-boolean resource-id)
      (to-boolean parsed-ticket)
      (not (re-find #"\.\." resource)) ; the resource should not contain ".."
      (= ((config/get-config) :presentation-type) (parsed-ticket :presentation-type))
      (= ip-address (parsed-ticket :ip-address))
      (to-boolean
        (some #{resource-id}
          (map shorten-resource-id (parsed-ticket :resource-ids)))))))
