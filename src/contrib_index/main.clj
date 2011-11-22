(ns contrib-index.main
  (:use [contrib-index.indices :only [all-indices-by-repo]]
        [contrib-index.build-html :only [make-all-pages]]))

(defn -main []
  (make-all-pages (filter #(not (#{"clojure-contrib"} (first %)))
                          (all-indices-by-repo "clojure" true))))
