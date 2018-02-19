(ns graphql-playground.schema
  (:require [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn resolve-game-by-id
  [games-map context args value]
  (let [{:keys [id]} args]
    (get games-map id)))

(defn resolve-member-by-id
  [members-map context args value]
  (let [{:keys [id]} args]
    (get members-map id)))

(defn resolve-board-game-designers
  [designers-map context args board-game]
  (->> board-game
       :designers
       (map designers-map)))

(defn resolve-designer-games
  [games-map context args designer]
  (let [{:keys [id]} designer]
    (->> games-map
         vals
         (filter #(-> % :designers (contains? id))))))

(defn resolve-board-game-rating-summary
  [ratings-map context args board-game]
  (let [{:keys [id]} board-game
        ratings      (filter #(= (:game_id %) id) ratings-map)
        count        (count ratings)]
    {:count   count
     :average (if (> count 0)
                (/ (reduce (fn [acc {:keys [rating]}] (+ acc rating)) 0 ratings) count)
                0.0)}))

(defn resolve-member-ratings
  [ratings-map games-map context args member]
  (let [{:keys [id]} member
        ratings      (filter #(= (:member_id %) id) ratings-map)]
    ratings))

(defn resolve-game-rating-game
  [games-map context args game-rating]
  (let [{:keys [game_id]} game-rating]
    (games-map game_id)))

(defn entity-map
  [data k]
  (reduce #(assoc %1 (:id %2) %2)
          {}
          (get data k)))

(defn resolver-map
  [component]
  (let [cgg-data      (-> (io/resource "cgg-data.edn")
                          slurp
                          edn/read-string)
        games-map     (entity-map cgg-data :games)
        designers-map (entity-map cgg-data :designers)
        members-map   (entity-map cgg-data :members)
        ratings-map   (cgg-data :ratings)]
    {:query/game-by-id         (partial resolve-game-by-id games-map)
     :query/member-by-id       (partial resolve-member-by-id members-map)
     :BoardGame/designers      (partial resolve-board-game-designers designers-map)
     :BoardGame/rating-summary (partial resolve-board-game-rating-summary ratings-map)
     :GameRating/game          (partial resolve-game-rating-game games-map)
     :Member/ratings           (partial resolve-member-ratings ratings-map games-map)
     :Designer/games           (partial resolve-designer-games games-map)}))

(defn load-schema [component]
  (-> (io/resource "cgg-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map component))
      schema/compile))

(defrecord SchemaProvider [schema]
  component/Lifecycle
  (start [this]
    (assoc this :schema (load-schema this)))

  (stop [this]
    (assoc this :schema nil)))

(defn new-schema-provider
  []
  {:schema-provider (map->SchemaProvider {})})
