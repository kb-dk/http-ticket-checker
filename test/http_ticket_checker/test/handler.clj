(ns http-ticket-checker.test.handler
  (:use clojure.test
        ring.mock.request  
        http-ticket-checker.handler))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 404))))

  (testing "not-found route"
    (let [response (app (request :get "/resource-without-ticket"))]
      (is (= (:status response) 403)))))