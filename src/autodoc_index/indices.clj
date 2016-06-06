(ns autodoc-index.indices
  "Code for accessing github and building an index of all autodoc for an organization"
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:use [clojure.pprint :only [cl-format]])
  (:import [java.io File]))

(defn user-url
  ([user] (user-url user false))
  ([user-or-org org?]
     (cl-format nil "https://api.github.com/~:[users~;orgs~]/~a"
                org? user-or-org)))
(defn repos
  [user org? auth-str]
  (let [url (str (user-url user org?) "/repos?page=1&per_page=10000")
        result (-> (client/get url {:accept :json :basic-auth auth-str}) :body
                   (#(java.io.StringReader. %)) json/read-json)
        ;; Github will return other users repos that you have rights to, so
        ;; filter those out.
        result (filter #(= user (-> % :owner :login)) result)]
    (for [r result] (:name r))))

(defn branches [user repo auth-str]
  (let [url (cl-format nil "https://api.github.com/repos/~a/~a/branches?page=1&per_page=10000" user repo)]
   (for [r (-> (client/get url {:accept :json :basic-auth auth-str}) :body
               (#(java.io.StringReader. %)) json/read-json)]
     (:name r))))

(defn gh-pages-sha [user repo auth-str]
  (binding [*out* *err*] (println "gh-pages-sha" user repo))
  (let [url (cl-format nil "https://api.github.com/repos/~a/~a/branches?page=1&per_page=10000" user repo)
        branches (-> (client/get url {:accept :json :basic-auth auth-str})
                     :body
                     (#(java.io.StringReader. %)) json/read-json)]
   (when-let [branch (first (filter #(= "gh-pages" (:name %)) branches)) ]
     (get-in branch [:commit :sha]))))


(defn index-file [user repo auth-str]
  (when-let [sha (gh-pages-sha user repo auth-str)]
    (let [url (cl-format nil "https://api.github.com/repos/~a/~a/git/trees/~a?page=1&per_page=10000" user repo sha)
          result (-> (client/get url {:accept :json :basic-auth auth-str}) :body
                     (#(java.io.StringReader. %)) json/read-json)
          tree (:tree result)]
      (when-let [file-desc (last
                            (sort-by :path
                                     (filter #(re-matches #"^index-.*\.clj$"
                                                          (:path %))
                                             tree)))]
        (let [file-url (:url file-desc)]
          (-> (client/get file-url {:accept "application/vnd.github.beta.raw"
                                    :basic-auth auth-str})
              :body read-string))))))

(defn get-auth-str
  "Get the authentication info from the file ~/.github-auth. It should
   be in the form username:password"
  []
  (str/trim-newline
   (slurp (File. (System/getenv "HOME") ".github-auth"))))

(defn all-indices-by-repo [user org?]
  (let [auth-str (get-auth-str)]
    (for [repo  (sort (repos user org? auth-str))]
      [repo (index-file user repo auth-str)])))

(defn one-namespace?
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
                (for [v (:vars data) :when (:name v)] ; skip specs for now
                  (assoc v
                    :project project
                    :one-namespace? (one-namespace? data))))))))
