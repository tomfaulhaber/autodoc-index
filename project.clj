(defproject autodoc-index "0.2.0-SNAPSHOT"
  :description "Build an overview of Clojure and all the documented contrib libraries based on the generated autodoc"
  :main autodoc-index.main
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.4"]
                 [enlive "1.0.0"]
                 [clj-http "0.9.2"]]
  :dev-dependencies [])
