(ns http-ticket-checker.test.handler
  (:use clojure.test
        ring.mock.request  
        http-ticket-checker.handler)
  (:require [http-ticket-checker.configuration :as config]
            [http-ticket-checker.tickets :as tickets]
            [clojure.data.json :as json]))

;; set the minimum necessary config
(config/set-config {:presentation_type "Thumbnails"})

(def ticket-a
  {"resources" ["doms_reklamefilm:uuid:35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec"]
   "type" "Thumbnails"
   "userIdentifier" "127.0.0.1"})

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 404))))

  (testing "reload-route is inaccessible from a remote host"
    (let [response (app (assoc (request :get "/reload") :remote-addr "1.2.3.4"))]
      (is (= (:status response) 403))))

  (testing "not-found route"
    (let [response (app (request :get "/resource-without-ticket"))]
      (is (= (:status response) 403))))

  (testing "valid-ticket? with valid ticket"
    (let [ticket (json/write-str ticket-a)
          resource "3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg"
          user-identifier (ticket-a "userIdentifier")]
      (is (tickets/valid-ticket? resource ticket user-identifier))))

  (testing "valid-ticket? with invalid user-identifier"
    (let [ticket (json/write-str ticket-a)
          resource "3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg"
          user-identifier "1.2.3.4"]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? with invalid resource"
    (let [ticket (json/write-str ticket-a)
          resource "3/5/a/1/giraffe.snapshot.0.jpeg"
          user-identifier (ticket-a "userIdentifier")]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? with invalid presentation-type"
    (let [ticket (json/write-str (assoc ticket-a "type" "elephant"))
          resource "3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg"
          user-identifier (ticket-a "userIdentifier")]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? without ticket"
    (let [ticket nil
          resource "3/5/a/1/35a1aa76-97a1-4f1b-b5aa-ad2a246eeeec.snapshot.0.jpeg"
          user-identifier (ticket-a "userIdentifier")]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  )