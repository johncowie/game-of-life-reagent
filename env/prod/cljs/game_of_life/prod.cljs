(ns game-of-life.prod
  (:require [game-of-life.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
