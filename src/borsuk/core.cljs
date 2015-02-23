(ns borsuk.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [borsuk.connection :refer [->RiemannConnection]]
            [om.core :as om]
            [om-tools.dom :as dom]))


(enable-console-print!)

(def app-state
  (atom
    {:feeds [{:title "all" :max 10 :type :log :query "true" :host "127.0.0.1" :port 5556}
             {:title "second" :max 10 :type :log :query "service = \"riemann streams rate\""
              :host "127.0.0.1" :port 5556}]
     :events {}
     :keymap {}}))

(defn parse-event [event]
  (when event
    (let [{:strs [host service state metric]} (js->clj (js/JSON.parse event))]
      [host service state metric])))

(defn row-event [row]
  (dom/tr nil
    (mapv #(dom/td nil %) row)))

(defn riemann-graph [{:keys [title max type query host port] :as cursor} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:close (chan)})
    om/IWillMount
    (will-mount [_]
      (let [conn (->RiemannConnection
                   {:host host :port 5556 :query query})]
        (go-loop []
          (let [new-event (<! (:recv conn))]
            (om/transact! cursor [:events title]
              (fn [events] (if (> (count events) max)
                             (cons new-event (butlast events))
                             (cons new-event events)))))
          (recur))))
    om/IRenderState
    (render-state [this _]
      (dom/div nil
        (dom/h2 nil title)
        (dom/table {:class "log"}
          (mapv #(-> % :message parse-event row-event)
            (get-in @cursor [:events title])))))))

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
          (om/build-all riemann-graph (:feeds data)
            {:init-state state}))))))

(defn main []
  (om/root riemann-workspace app-state
    {:target (. js/document (getElementById "app"))}))

(main)
