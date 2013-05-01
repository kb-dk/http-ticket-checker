(defproject dk.statsbiblioteket.medieplatform/http-ticket-checker "1.1"
  :description "HTTP-ticket-checker is a web-application that serves resources iff the supplied ticket is valid.
  Tickets are retrieved from memcached, deserialized from json, and compared with the requested resource,
  client ip-address, and the configured presentation-type."
  :url "http://statsbiblioteket.github.io/http-ticket-checker/"
  :license {:name "The Apache Software License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo
            :comments "A business-friendly OSS license"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [com.taoensso/timbre "1.6.0"]
                 [compojure "1.1.5"]
                 [clojurewerkz/spyglass "1.0.2"]]
  :scm {:url "git@github.com:statsbiblioteket/http-ticket-checker.git"}
  :pom-addition [:developers [:developer
                              [:name "Adam Tulinius"]
                              [:email "aft@statsbiblioteket.dk"]
                              [:timezone "+1"]]]
  :plugins [[lein-ring "0.8.2"]
            [lein-marginalia "0.7.1"]]
  :ring {:handler http-ticket-checker.handler/app
         :init http-ticket-checker.handler/init
         :destroy http-ticket-checker.handler/destroy}
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}})
