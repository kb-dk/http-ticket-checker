;; ## Handling of configuration.

(ns http-ticket-checker-clj.configuration)


(def config
  "Atom which holds the configuration."
  (atom nil))

(defn get-config
  "Get the entire configuration, or get a specific value by passing
  in the name of it."
  ([] (deref config))
  ([key] ((get-config) key)))

(defn set-config
  [new-config]
  (swap! config
    (fn [_] new-config)))

(defn load-config
  "Load configuration from the file specificed by the environment
  variable `\"HTTP_TICKET_CHECKER_CONFIG\"`"
  []
  (load-file
    (System/getenv "HTTP_TICKET_CHECKER_CONFIG")))
