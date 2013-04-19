(ns http-ticket-checker.test.handler
  (:use clojure.test
        ring.mock.request)
  (:require [http-ticket-checker.handler :as handler]))


(deftest test-app
  (testing "main route"
    (let [response (handler/app (request :get "/"))]
      (is (= (:status response) 404))))

  (testing "reload-route is inaccessible from a remote host"
    (let [response (handler/app (assoc (request :get "/reload") :remote-addr "1.2.3.4"))]
      (is (= (:status response) 403))))

  (testing "not-found route"
    (let [response (handler/app (request :get "/resource-without-ticket"))]
      (is (= (:status response) 403)))))
