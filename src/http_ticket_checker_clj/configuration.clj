;; ## Retrieval of configurations

(ns http-ticket-checker-clj.configuration)


(def config-atom
  (atom nil))

(defn get-config []
  (deref config-atom))

(defn set-config [config]
  (swap! config-atom
    (fn [_] config)))

; Load configuration from file specificed by environment variable.
(defn load-config []
  (load-file
    (System/getenv "HTTP_TICKET_CHECKER_CONFIG")))

(defn get-config-param [param]
  ((get-config) param))

(defn use-x-sendfile []
  (get-config-param :use_x_sendfile))
