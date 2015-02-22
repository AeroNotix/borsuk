(ns borsuk.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [goog.net.WebSocket :as ws]
            [om.core :as om]
            [om-tools.dom :as dom])
  (:import [goog.net WebSocket]))


(enable-console-print!)

(def riemann-addr
  "ws://127.0.0.1:5556/index?subscribe=true&query=service%20%3D%20%22airos.uptime_counters%22")

(defn handle-message [type chan event]
  (fn [event]
    (let [msg (.-message event)]
      (println type msg)
      (put! chan {:msg-type type :message msg}))))

(def on-msg   (partial handle-message :msg))
(def on-open  (partial handle-message :open))
(def on-close (partial handle-message :closed))
(def on-err   (partial handle-message :error))

(defn ->WebSocket [addr]
  (let [msg-chan (chan)
        new-ws (WebSocket. nil nil)
        handlers [[ws/EventType.MESSAGE on-msg]
                  [ws/EventType.OPENED  on-open]
                  [ws/EventType.CLOSED  on-close]
                  [ws/EventType.ERROR   on-err]]]
    (doseq [[event-type event-handler] handlers]
      (.addEventListener new-ws event-type (event-handler msg-chan)))
    (.open new-ws addr)
    [new-ws msg-chan]))

(def connection (->WebSocket riemann-addr))
