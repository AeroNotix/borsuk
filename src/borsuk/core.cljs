(ns borsuk.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [borsuk.formatter :refer [format]]
            [cemerick.url :refer [url url-encode]]
            [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [goog.net.WebSocket :as ws]
            [om.core :as om]
            [om-tools.dom :as dom])
  (:import [goog.net WebSocket]))


(enable-console-print!)

(def base-address "ws://%s:%d/index/")

(defn handle-message [type chan event]
  (fn [event]
    (let [msg (.-message event)]
      (println type msg)
      (put! chan {:msg-type type :message msg}))))

(def on-msg   (partial handle-message :msg))
(def on-open  (partial handle-message :open))
(def on-close (partial handle-message :closed))
(def on-err   (partial handle-message :error))

(defn ->RiemannConnection [{:keys [host port query]}]
  (let [addr (-> (format base-address host port)
               url
               (assoc :query {:subscribe true :query query})
               str)
        recv-chan (chan)
        new-ws (WebSocket. nil nil)
        handlers [[ws/EventType.MESSAGE on-msg]
                  [ws/EventType.OPENED  on-open]
                  [ws/EventType.CLOSED  on-close]
                  [ws/EventType.ERROR   on-err]]]
    (doseq [[event-type event-handler] handlers]
      (.addEventListener new-ws event-type (event-handler recv-chan)))
    (.open new-ws addr)
    {:conn new-ws :recv recv-chan}))

(def connection
  (->RiemannConnection
    {:host "127.0.0.1" :port 5556 :query "true"}))

(go-loop []
  (println (<! (:chan connection))))
