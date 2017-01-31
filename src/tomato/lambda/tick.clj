(ns tomato.lambda.tick
  (:require [clojure.string :as s]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [tomato.core :as tomato]
            [tomato.handler :as handler])
  (:gen-class
    :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))


(defn key->keyword [key-string]
  (-> key-string
      (s/replace #"([a-z])([A-Z])" "$1-$2")
      (s/replace #"([A-Z]+)([A-Z])" "$1-$2")
      (s/lower-case)
      (keyword)))

(defn -handleRequest [_this is _os _context]
  (do
    (println (str "is") (json/parse-string (slurp (io/reader is)) key->keyword))
    (tomato/send-m! "tick-toc")))
