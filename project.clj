(defproject http-ticket-checker "0.1.0-SNAPSHOT"
  :description "HTTP-ticket-checker is a web-application that serves resources iff the supplied ticket is valid.
  Tickets are retrieved from memcached, deserialized from json, and compared with the requested resource,
  client ip-address, and the configured presentation-type."
  :url "http://statsbiblioteket.github.io/http-ticket-checker/"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [compojure "1.1.5"]
                 [clojurewerkz/spyglass "1.0.2"]]
  :plugins [[lein-ring "0.8.2"]
            [lein-marginalia "0.7.1"]]
  :ring {:handler http-ticket-checker.handler/app
         :init http-ticket-checker.handler/init
         :destroy http-ticket-checker.handler/destroy}
  :profiles {:dev {:dependencies [[ring-mock "0.1.3"]]}})
