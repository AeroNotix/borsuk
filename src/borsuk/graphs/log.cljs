(ns borsuk.graphs.log
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [borsuk.connection :refer [->RiemannConnection]]
            [om.core :as om]
            [borsuk.graphs.base :refer [parse-event row-event]]
            [om-tools.dom :as dom]))


(defn ->Log [{:keys [title max type query host port] :as cursor} owner]
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
