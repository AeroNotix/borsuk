(ns borsuk.core
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [borsuk.connection :refer [->RiemannConnection]]
            [borsuk.graphs.log :refer [->Log]]
            [om.core :as om]
            [om-tools.dom :as dom]))


(enable-console-print!)

(def app-state
  (atom
    {:feeds [{:title "all" :max 10 :graph-type :log :query "true" :host "127.0.0.1" :port 5556}
             {:title "second" :max 10 :graph-type :log :query "service = \"riemann streams rate\""
              :host "127.0.0.1" :port 5556}]
     :events {}
     :keymap {}}))

(defn graph-dispatcher [{:keys [graph-type] :as opts} state]
  ((graph-type {:log ->Log}) opts state))

(defn riemann-workspace [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:remove-graph (chan)
       :new-graph (chan)})
    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (dom/h2 nil "RIEMANN")
        (apply dom/ul nil
          (om/build-all graph-dispatcher (:feeds data)
            {:init-state state}))))))

(defn main []
  (om/root riemann-workspace app-state
    {:target (. js/document (getElementById "app"))}))

(main)
