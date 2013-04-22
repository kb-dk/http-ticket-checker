(ns http-ticket-checker.test.setup
  (:use clojure.test
        ring.mock.request)
  (:require [http-ticket-checker.configuration :as config]
            [http-ticket-checker.handler :as handler]
            [http-ticket-checker.tickets :as tickets]
            [clojure.data.json :as json]))


;; set the minimum necessary config
(config/set-config {:presentation_type "Thumbnails"})

(defn create-mock-ticket-store
  []
  {:get #((deref ((tickets/get-ticket-store) :store)) %1)
   :set #(swap! ((tickets/get-ticket-store) :store) assoc %1 %2)
   :store (atom {})})

(defn set-ticket
  [key value]
  (let [setter ((tickets/get-ticket-store) :set)]
    (setter key value)))

(tickets/set-ticket-store (create-mock-ticket-store))

(def thumbnail-a "/3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg")
(def thumbnail-b "/3/5/a/1/elephant.snapshot.0.jpeg")
(def long-resource-id-a "doms_reklamefilm:uuid:35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec")
(def long-resource-id-b "doms_reklamefilm:uuid:elephant")
(def presentation-type-a "Thumbnails")
(def presentation-type-b "Stream")
(def user-identifier-a "localhost")
(def user-identifier-b "130.225.24.24")

(defn create-ticket
  [resources presentation-type user-identifier]
  {"resources" resources
   "type" presentation-type
   "userIdentifier" user-identifier})

(def ticket-a
  (create-ticket [long-resource-id-a] presentation-type-a user-identifier-a))

(def ticket-b
  (create-ticket [long-resource-id-b] presentation-type-b user-identifier-b))

(set-ticket "ticket-a" (json/write-str ticket-a))
(set-ticket "ticket-b" (json/write-str ticket-b))
