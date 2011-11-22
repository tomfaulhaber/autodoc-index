(ns contrib-index.indices
  "A sandbox for playing with the code that we'll use for creating indices"
  (:require [clj-http.client :as client]
            [clj-json.core :as json])
  (:use [clojure.pprint :only [cl-format]]))

(defn user-url
  ([user] (user-url user false))
  ([user-or-org org?]
     (cl-format nil "https://api.github.com/~:[users~;orgs~]/~a"
                org? user-or-org)))
(defn repos
  [base]
  (let [url (str base "/repos") 
        result (-> (client/get url {:accept :json}) :body json/parse-string)]
    (for [r result] (get r "name"))))

(defn branches [user repo]
  (let [url (cl-format nil "https://api.github.com/repos/~a/~a/branches" user repo)]
   (for [r (-> (client/get url {:accept :json}) :body json/parse-string)]
     (get r "name"))))

(defn gh-pages-sha [user repo]
  (let [url (cl-format nil "https://api.github.com/repos/~a/~a/branches" user repo)
        branches (-> (client/get url {:accept :json}) :body json/parse-string)]
   (when-let [branch (first (filter #(= "gh-pages" (get % "name")) branches)) ]
     (get-in branch ["commit" "sha"]))))


(defn index-file [user repo]
  (when-let [sha (gh-pages-sha user repo)]
    (let [url (cl-format nil "https://api.github.com/repos/~a/~a/git/trees/~a" user repo sha)
          result (-> (client/get url {:accept :json}) :body json/parse-string)
          tree (get result "tree")]
      (when-let [file-desc (last
                            (sort-by #(get % "path")
                                     (filter #(re-matches #"^index-.*\.clj$"
                                                          (get % "path"))
                                             tree)))]
        (let [file-url (get file-desc "url")]
          (-> (client/get file-url {:accept "application/vnd.github.beta.raw"})
              :body read-string))))))

(defn all-indices-by-repo [user org?]
  (for [repo  (sort (repos (user-url user org?)))]
    [repo (index-file user repo) ]))

(defn- one-namespace?
  "Takes a list of namespaces and returns true if there's only a single
top-level namespace"
  [data]
  (let [namespaces (set (map (comp #(into [] (.split % "\\."))  :name)
                             (:namespaces data)))
        my-prefixes (fn [arr] (let [c (count arr)] (for [n (range 1 c)] (take n arr))))
        top-level (filter (fn [ns] (every? #(not (namespaces %)) (my-prefixes ns))) namespaces)]
    (< (count top-level) 2)))

(defn flatten-vars
  "From a vector of indices by repo create a flat list of variables each with the
repo name added on a :project key."
  ([indices]
     (apply concat
            (for [[project data] indices]
              (do
                (for [v (:vars data)]
                  (assoc v
                    :project project
                    :one-namespace? (one-namespace? data))))))))
