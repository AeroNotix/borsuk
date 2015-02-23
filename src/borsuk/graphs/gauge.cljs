(ns borsuk.graphs.gauge
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [borsuk.connection :refer [->RiemannConnection]]
            [om.core :as om]
            [borsuk.graphs.base :refer [message->map]]
            [om-tools.dom :as dom]))


(defn ->Gauge [{:keys [title max type query host port] :as cursor} owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [conn (->RiemannConnection
                   {:host host :port 5556 :query query})]
        (go-loop []
          (let [new-event (<! (:recv conn))
                metric (-> new-event :message message->map (get "metric"))]
            (when metric
              (om/update! cursor [:events title] metric))
            (recur)))))
    om/IRenderState
    (render-state [this _]
      (dom/div nil
        (dom/h2 nil title)
        (dom/h2 nil (get-in @cursor [:events title]))))))
