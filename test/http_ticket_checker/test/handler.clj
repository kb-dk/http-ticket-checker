(ns http-ticket-checker.test.handler
  (:use clojure.test
        ring.mock.request
        http-ticket-checker.test.setup
        http-ticket-checker.test.tickets)
  (:require [http-ticket-checker.handler :as handler]
            [http-ticket-checker.tickets :as tickets]
            [clojure.data.json :as json]))


(defn get-content [resource ticket-id]
  (let [req (query-string
              (request :get resource)
              {"ticket" ticket-id})]
    (handler/app req)))

(deftest test-app
  (testing "main route"
    (let [response (handler/app (request :get "/"))]
      (is (= (:status response) 404))))

  (testing "reload-route is inaccessible from a remote host"
    (let [response (handler/app (assoc (request :get "/reload") :remote-addr "1.2.3.4"))]
      (is (= (:status response) 403))))

  ;; todo: returning 404 since the resource isn't mocked, go fix it :-)
  (testing "request content with valid ticket"
    (let [response (get-content thumbnail-a "ticket-a")]
      (is (= (:status response) 404))))

  (testing "request content without ticket"
    (let [response (get-content thumbnail-a "")]
      (is (= (:status response) 403))))

  (testing "request content with invalid ticket"
    (let [response (get-content thumbnail-a "ticket-b")]
      (is (= (:status response) 403)))))
