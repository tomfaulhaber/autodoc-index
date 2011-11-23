(ns autodoc-index.main
  (:use [autodoc-index.indices :only [all-indices-by-repo]]
        [autodoc-index.build-html :only [make-all-pages]]))

(defn -main []
  (make-all-pages (filter #(not (#{"clojure-contrib"} (first %)))
                          (all-indices-by-repo "clojure" true))))
