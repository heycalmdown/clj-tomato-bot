(ns tomato.s3
  (:require [amazonica.aws.s3 :as s3]
            [clojure.edn :as edn]
            [tomato.config :as config]))

(defn reset! [k v]
  (when (config/get :s3)
    (s3/put-object (config/get :s3) "clj-tomato-bot" k (pr-str v))))

(defn read [k]
  (when (config/get :s3)
    (try (edn/read-string
           (slurp
             (:object-content (s3/get-object (config/get :s3) "clj-tomato-bot" k))))
         (catch com.amazonaws.services.s3.model.AmazonS3Exception _ ()))))
