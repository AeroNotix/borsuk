(ns borsuk.graphs.base
  (:require [om-tools.dom :as dom]))

(defn parse-event [event]
  (when event
    (let [{:strs [host service state metric]} (js->clj (js/JSON.parse event))]
      [host service state metric])))

(defn row-event [row]
  (dom/tr nil
    (mapv #(dom/td nil %) row)))
