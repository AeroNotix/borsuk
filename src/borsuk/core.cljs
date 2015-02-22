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

(def app-state
  (atom
    {:feeds [{:title "all" :max 10 :type :log :query "true" :host "127.0.0.1" :port 5556}
             {:title "second" :max 10 :type :log :query "service = \"riemann streams rate\"" :host "127.0.0.1" :port 5556}]
     :events {}
     :keymap {}}))

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

(defn riemann-graph [{:keys [title type query host port]} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:close (chan)})
    om/IWillMount
    (will-mount [_]
      (let [conn (->RiemannConnection
                   {:host host :port 5556 :query query})]
        (go-loop []
          (println (<! (:recv conn)))
          (recur))))
    om/IRenderState
    (render-state [this {:keys [conn]}]
      (take! (:recv conn) println))))

(defn riemann-workspace [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:remove-graph (chan)
       :new-graph (chan)})
    om/IRenderState
    (render-state [this state]
      (dom/h2 "RIEMANN")
      (apply dom/ul nil
        (om/build-all riemann-graph (:feeds data)
          {:init-state state})))))

(defn main []
  (om/root riemann-workspace app-state
    {:target (. js/document (getElementById "app"))}))

(main)
