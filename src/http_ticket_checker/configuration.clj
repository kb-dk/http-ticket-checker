;; ## Handling of configuration.

(ns http-ticket-checker.configuration)


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
  (reset! config new-config))

(defn load-config
  "Load configuration either from the file specificed by the environment
  variable `\"HTTP_TICKET_CHECKER_CONFIG\"` or from the file name specified."
  ([]
    (load-file
      (System/getenv "HTTP_TICKET_CHECKER_CONFIG")))
  ([filename]
    (load-file filename)))
