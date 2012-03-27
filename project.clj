(defproject autodoc-index "0.1.0-SNAPSHOT"
  :description "Build an overview of Clojure and all the documented contrib libraries based on the generated autodoc"
  :main autodoc-index.main
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.3"]
                 [enlive "1.0.0"]
                 [clj-http "0.3.3"]]
  :dev-dependencies [])
