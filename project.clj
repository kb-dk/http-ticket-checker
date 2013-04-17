(defproject http-ticket-checker-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [compojure "1.1.5"]
                 [clojurewerkz/spyglass "1.0.2"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler http-ticket-checker-clj.handler/app
         :init http-ticket-checker-clj.handler/init
         :destroy http-ticket-checker-clj.handler/destroy}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
