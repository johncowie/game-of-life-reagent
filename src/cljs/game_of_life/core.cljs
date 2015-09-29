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
    (.log js/console n)
    (.log js/console m)
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
  (->>
    (map (partial update-cell m) m)
    (into {})))

(def game (atom (create-map 4 4)))

(js/setInterval (swap! game tick) 1000)

(defn home-page []
  [:div [:h2 "Welcome to game-of-life"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About game-of-life"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn get-fill [status]
  (status {:on on :off off}))

(defn render-game [game]
  [:svg.svg {:width 400 :height 400}
     (for [[[x y] status] @game]
         [:rect {:width 100 :height 100 :x (* x 100) :y (* y 100) :fill (get-fill status)}])
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
