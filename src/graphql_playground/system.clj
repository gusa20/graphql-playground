(ns graphql-playground.system
  (:require
   [com.stuartsierra.component :as component]
   [graphql-playground.schema :as schema]
   [graphql-playground.server :as server]))

(defn new-system
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)))
