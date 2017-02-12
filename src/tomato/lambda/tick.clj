(ns tomato.lambda.tick
  (:require [clojure.string :as s]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [tomato.core :as tomato]
            [tomato.handler :as handler]
            [tomato.cloudwatch :as cwe])
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
    (let [input (json/parse-string (slurp (io/reader is)) key->keyword)
          state (tomato/get-state!)
          remaining (tomato/remaining-secs! state)
          mode ((:mode state) tomato/modes)]
      (println (str "input v3") input)
      (println (str "state" state))
      (if (:timer state)
        (do
          (tomato/send-remaining! state "tick")
          (println remaining)
          (if (< remaining 5)
            (tomato/goto-x! (:next mode)))))

      ;(cwe/cancel-timeout! (:cur input))
      ;(tomato/goto-x! (keyword (:next input)))
      )))
