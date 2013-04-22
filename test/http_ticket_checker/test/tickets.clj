(ns http-ticket-checker.test.tickets
  (:use clojure.test
        http-ticket-checker.test.setup)
  (:require [http-ticket-checker.configuration :as config]
            [http-ticket-checker.tickets :as tickets]
            [clojure.data.json :as json]))


(deftest test-ticket-validation
  (testing "valid-ticket? with valid ticket"
    (let [ticket (tickets/get-ticket "ticket-a")
          resource thumbnail-a
          user-identifier user-identifier-a]
      (is (tickets/valid-ticket? resource ticket user-identifier))))

  (testing "valid-ticket? with invalid ticket"
    (let [ticket (tickets/get-ticket "ticket-a")
          resource thumbnail-b
          user-identifier user-identifier-a]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? without ticket"
    (let [ticket nil
          resource thumbnail-a
          user-identifier user-identifier-a]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? with invalid user-identifier"
    (let [ticket (tickets/get-ticket "ticket-a")
          resource thumbnail-a
          user-identifier user-identifier-b]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? with invalid resource"
    (let [ticket (tickets/get-ticket "ticket-a")
          resource thumbnail-b
          user-identifier (ticket-a "userIdentifier")]
      (is (not (tickets/valid-ticket? resource ticket user-identifier)))))

  (testing "valid-ticket? with invalid presentation-type"
    (let [ticket (json/write-str (assoc ticket-a "type" "elephant"))
          resource thumbnail-a
          user-identifier (ticket-a "userIdentifier")]
      (is (not (tickets/valid-ticket? resource ticket user-identifier))))))

(deftest test-shorten-resource-id
  (testing "foo"
    (let [id "uuid:12345"
          target "12345"]
      (is (= (tickets/shorten-resource-id id) target)))))