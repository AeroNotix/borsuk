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
    {:feeds [{:title "all" :type :log :query "true" :host "127.0.0.1" :port 5556}]
     :keymap {}}))

(def base-address "ws://%s:%d/index/")

(defn handle-message [type chan event]
  (fn [event]
    (let [msg (.-message event)]
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

(defn riemann-graph [{:keys [title type query host port]} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:close (chan)})
    om/IWillMount
    (will-mount [_]
      (let [conn (->RiemannConnection
                   {:host host :port 5556 :query "true"})]
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
