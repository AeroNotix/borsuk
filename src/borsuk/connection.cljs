(ns borsuk.connection
  (:require [cljs.core.async :as async :refer [<! >! take! put! chan]]
            [cemerick.url :refer [url url-encode]]
            [borsuk.formatter :refer [format]]
            [goog.net.WebSocket :as ws])
  (:import [goog.net WebSocket]))


(def base-address "ws://%s:%d/index/")

(def message-types
  {ws/EventType.MESSAGE :msg
   ws/EventType.OPENED  :open
   ws/EventType.CLOSED  :closed
   ws/EventType.ERROR   :error})

(defn handle-message [msg-type chan]
  (let [msg-type (get message-types msg-type)]
    (fn [event]
      (let [msg (.-message event)]
        (put! chan {:type msg-type :message msg})))))

(defn ->RiemannConnection
  [{:keys [host port query] :or {host "127.0.0.1" port 5556 query true}}]
  (let [addr (-> (format base-address host port)
               url
               (assoc :query {:subscribe true :query query})
               str)
        recv-chan (chan)
        new-ws (WebSocket. nil nil)
        handlers (keys message-types)]
    (doseq [event-type handlers]
      (.addEventListener new-ws event-type
        (handle-message event-type recv-chan)))
    (.open new-ws addr)
    {:conn new-ws :recv recv-chan}))
