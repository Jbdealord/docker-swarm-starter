(ns swarmpit.component.service.form-ports
  (:require [material.component :as comp]
            [material.component.form :as form]
            [material.component.list-table-form :as list]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :ports])

(defn- not-suggested?
  [port]
  (not
    (some #(= (:containerPort port)
              (:containerPort %)) (state/get-value cursor))))

(defn load-suggestable-ports
  [repository]
  (handler/get
    (routes/path-for-backend :repository-ports)
    {:params     {:repository    (:name repository)
                  :repositoryTag (:tag repository)}
     :on-success (fn [response]
                   (doseq [port response]
                     (if (not-suggested? port)
                       (state/add-item port cursor))))
     :on-error   (fn [_])}))

(def headers [{:name  "Container port"
               :width "100px"}
              {:name  "Protocol"
               :width "100px"}
              {:name  "Host port"
               :width "100px"}])

(defn- format-port-value
  [value]
  (if (or (zero? value)
          (js/isNaN value)) "" value))

(defn- form-container [value index]
  (list/textfield
    {:name     (str "form-container-text-" index)
     :key      (str "form-container-text-" index)
     :type     "number"
     :min      1
     :max      65535
     :value    (format-port-value value)
     :onChange (fn [_ v]
                 (state/update-item index :containerPort (js/parseInt v) cursor))}))

(defn- form-protocol [value index]
  (list/selectfield
    {:name     (str "form-protocol-select-" index)
     :key      (str "form-protocol-select-" index)
     :value    value
     :onChange (fn [_ _ v]
                 (state/update-item index :protocol v cursor))}
    (comp/menu-item
      {:name        (str "form-protocol-tcp-" index)
       :key         (str "form-protocol-tcp-" index)
       :value       "tcp"
       :primaryText "TCP"})
    (comp/menu-item
      {:name        (str "form-protocol-udp-" index)
       :key         (str "form-protocol-udp-" index)
       :value       "udp"
       :primaryText "UDP"})))

(defn- form-host [value index]
  (list/textfield
    {:name     (str "form-host-text-" index)
     :key      (str "form-host-text-" index)
     :type     "number"
     :min      1
     :max      65535
     :value    (format-port-value value)
     :onChange (fn [_ v]
                 (state/update-item index :hostPort (js/parseInt v) cursor))}))

(defn- render-ports
  [item index _]
  (let [{:keys [containerPort
                protocol
                hostPort]} item]
    [(form-container containerPort index)
     (form-protocol protocol index)
     (form-host hostPort index)]))

(defn- form-table
  [ports]
  (list/table headers
              ports
              nil
              render-ports
              (fn [index] (state/remove-item index cursor))))

(defn- add-item
  []
  (state/add-item {:containerPort 0
                   :protocol      "tcp"
                   :hostPort      0} cursor))

(rum/defc form < rum/reactive []
  (let [ports (state/react cursor)]
    (if (empty? ports)
      (form/value "Service has no published ports.")
      (form-table ports))))