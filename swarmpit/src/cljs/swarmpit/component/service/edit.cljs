(ns swarmpit.component.service.edit
  (:require [material.icon :as icon]
            [material.component :as comp]
            [material.component.form :as form]
            [material.component.panel :as panel]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.progress :as progress]
            [swarmpit.component.service.form-settings :as settings]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.component.service.form-networks :as networks]
            [swarmpit.component.service.form-mounts :as mounts]
            [swarmpit.component.service.form-secrets :as secrets]
            [swarmpit.component.service.form-configs :as configs]
            [swarmpit.component.service.form-variables :as variables]
            [swarmpit.component.service.form-labels :as labels]
            [swarmpit.component.service.form-logdriver :as logdriver]
            [swarmpit.component.service.form-resources :as resources]
            [swarmpit.component.service.form-deployment :as deployment]
            [swarmpit.component.service.form-deployment-placement :as placement]
            [swarmpit.component.message :as message]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form])

(defonce loading? (atom false))

(defn render-item
  [val]
  (if (boolean? val)
    (comp/checkbox {:checked val})
    val))

(defn- service-handler
  [service-id]
  (handler/get
    (routes/path-for-backend :service {:id service-id})
    {:state loading?
     :on-success
            (fn [service]
              (settings/tags-handler (-> service :repository :name))
              (state/set-value (select-keys service [:repository :version :serviceName :mode :replicas :stack]) settings/cursor)
              (state/set-value (:ports service) ports/cursor)
              (state/set-value (:mounts service) mounts/cursor)
              (state/set-value (->> (:secrets service)
                                    (map #(select-keys % [:secretName :secretTarget]))
                                    (into [])) secrets/cursor)
              (state/set-value (->> (:configs service)
                                    (map #(select-keys % [:configName :configTarget]))
                                    (into [])) configs/cursor)
              (state/set-value (:variables service) variables/cursor)
              (state/set-value (:labels service) labels/cursor)
              (state/set-value (:logdriver service) logdriver/cursor)
              (state/set-value (:resources service) resources/cursor)
              (state/set-value (:deployment service) deployment/cursor))}))

(defn- service-networks-handler
  [service-id]
  (handler/get
    (routes/path-for-backend :service-networks {:id service-id})
    {:on-success
     (fn [networks]
       (state/set-value (->> networks
                             (map #(select-keys % [:networkName :serviceAliases]))
                             (into [])) networks/cursor))}))

(defn- update-service-handler
  [service-id]
  (let [settings (state/get-value settings/cursor)
        ports (state/get-value ports/cursor)
        networks (state/get-value networks/cursor)
        secrets (state/get-value secrets/cursor)
        configs (state/get-value configs/cursor)
        variables (state/get-value variables/cursor)
        labels (state/get-value labels/cursor)
        logdriver (state/get-value logdriver/cursor)
        resources (state/get-value resources/cursor)
        deployment (state/get-value deployment/cursor)]
    (handler/post
      (routes/path-for-backend :service-update {:id service-id})
      {:params     (-> settings
                       (assoc :ports ports)
                       (assoc :networks networks)
                       (assoc :mounts (mounts/normalize))
                       (assoc :secrets (when (not (empty? @secrets/secrets-list)) secrets))
                       (assoc :configs (when (not (empty? @configs/configs-list)) configs))
                       (assoc :variables variables)
                       (assoc :labels labels)
                       (assoc :logdriver logdriver)
                       (assoc :resources resources)
                       (assoc :deployment deployment))
       :on-success (fn [_]
                     (dispatch!
                       (routes/path-for-frontend :service-info {:id service-id}))
                     (message/info
                       (str "Service " service-id " has been updated.")))
       :on-error   (fn [response]
                     (message/error
                       (str "Service update failed. Reason: " (:error response))))})))

(def mixin-init-form
  (mixin/init-form
    (fn [{{:keys [id]} :params}]
      (service-handler id)
      (service-networks-handler id)
      (mounts/volumes-handler)
      (networks/networks-handler)
      (secrets/secrets-handler)
      (when (<= 1.30 (state/get-value [:docker :api]))
        (configs/configs-handler))
      (placement/placement-handler)
      (labels/labels-handler))))

(rum/defc form-settings < rum/static []
  [:div.form-layout-group
   (form/section "General settings")
   (settings/form true)])

(rum/defc form-ports < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Ports" ports/add-item)
   (ports/form)])

(rum/defc form-networks < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Networks" networks/add-item)
   (networks/form)])

(rum/defc form-mounts < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Mounts" mounts/add-item)
   (mounts/form)])

(rum/defc form-secrets < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Secrets" secrets/add-item)
   (secrets/form)])

(rum/defc form-configs < rum/reactive []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Configs" configs/add-item)
   (configs/form)])

(rum/defc form-variables < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Environment Variables" variables/add-item)
   (variables/form)])

(rum/defc form-labels < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section-add "Labels" labels/add-item)
   (labels/form)])

(rum/defc form-logdriver < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section "Logging")
   (logdriver/form)])

(rum/defc form-resources < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section "Resources")
   (resources/form)])

(rum/defc form-deployment < rum/static []
  [:div.form-layout-group.form-layout-group-border
   (form/section "Deployment")
   (deployment/form)])

(rum/defc form-edit < rum/reactive [id settings]
  [:div
   [:div.form-panel
    [:div.form-panel-left
     (panel/info icon/services (:serviceName settings))]
    [:div.form-panel-right
     (comp/mui
       (comp/raised-button
         {:onTouchTap #(update-service-handler id)
          :label      "Save"
          :disabled   (not (rum/react resources/valid?))
          :primary    true}))
     [:span.form-panel-delimiter]
     (comp/mui
       (comp/raised-button
         {:href  (routes/path-for-frontend :service-info {:id id})
          :label "Back"}))]]
   [:div.form-layout
    (form-settings)
    (form-ports)
    (form-networks)
    (form-mounts)
    (form-secrets)
    (when (<= 1.30 (state/get-value [:docker :api]))
      (form-configs))
    (form-variables)
    (form-labels)
    (form-logdriver)
    (form-resources)
    (form-deployment)]])

(rum/defc form < rum/reactive
                 mixin-init-form [{{:keys [id]} :params}]
  (let [settings (state/react settings/cursor)]
    (progress/form
      (rum/react loading?)
      (form-edit id settings))))