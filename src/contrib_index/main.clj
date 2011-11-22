(ns contrib-index.main
  (:use [contrib-index.indices :only [all-indices-by-repo]]
        [contrib-index.build-html :only [make-all-pages]]))

(defn -main []
  (make-all-pages (all-indices-by-repo "clojure" true) #{"clojure-contrib"}))
