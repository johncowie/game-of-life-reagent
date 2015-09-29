(ns game-of-life.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

;; -------------------------
;; Views

(def on "#ff0000")
(def off "#0000ff")

(def game-width 40)
(def game-height 40)
(def square-size 10)

(defn create-map [w h]
  (into {}
    (for [x (range 0 w) y (range 0 h)]
      [[x y] (rand-nth [:on :off])])))

(defn adjacent-coords [[x y]]
  (remove nil?
    (for [i [(dec x) x (inc x)]
          j [(dec y) y (inc y)]]
      (when (not= [i j] [x y]) [i j]))))

(defn lookup-cell [m coord]
  (get m coord))

(defn count-living-neighbours [coord m]
  (->>
    (adjacent-coords coord)
    (map (partial lookup-cell m))
    (filter #(= :on %))
    count))

(defn update-cell [m [coord status]]
  (let [n (count-living-neighbours coord m)]
    (cond
      (and (= status :on) (< n 2)) [coord :off]
      (and (= status :on) (or (= n 2) (= n 3))) [coord :on]
      (and (= status :on) (> n 3)) [coord :off]
      (and (= status :off) (= n 3)) [coord :on]
      :else [coord status]
     )
    )
  )

(defn tick [m]
  (->> m
    (map (partial update-cell m))
    (into {})))

(def game (atom (create-map game-width game-height)))

(js/setInterval #(swap! game tick) 200)

(defn get-fill [status]
  (status {:on on :off off}))

(defn render-game [game]
  [:svg.svg {:width (* game-width square-size) :height (* game-height square-size)}
     (for [[[x y] status] @game]
       ^{:key [x y]} [:rect {:width square-size :height square-size
                             :x (* x square-size) :y (* y square-size)
                             :fill (get-fill status)}])
    ])

(defn game-of-life []
  [:div
   [:h1 "Game of life"]
   [render-game game]
   ])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'game-of-life))


;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
