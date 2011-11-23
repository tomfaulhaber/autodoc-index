(ns autodoc-index.build-html
  "A stripped down version of autodoc's build-html that is designed just to build the
global index files."
  (:refer-clojure :exclude [empty complement]) 
  (:import [java.util.jar JarFile]
           [java.io File FileWriter BufferedWriter StringReader]
           [java.util.regex Pattern])
  (:require [clojure.string :as str])
  (:use [net.cgrand.enlive-html]
        [clojure.java.io :only (as-file file writer)]
        [clojure.pprint :only (pprint cl-format)]
        [autodoc-index.indices :only (flatten-vars)]))

(def template-directory "templates/")
(def layout-file (str template-directory "layout.html"))
(def master-toc-file (str template-directory "master-toc.html"))
(def local-toc-file (str template-directory "local-toc.html"))

(def overview-file "overview.html")
(def index-html-file "api-index.html")

(def overview-file-template (str template-directory overview-file))
(def index-html-file-template (str template-directory index-html-file))

(def output-directory "../autodoc-work-area/clojure.github.com")

(deftemplate page layout-file
  [title master-toc local-toc page-content]
  [:html :head :title] (content title)
  [:a#page-header] (content title)
  [:div#leftcolumn] (content master-toc)
  [:div#right-sidebar] (content local-toc)
  [:div#content-tag] (content page-content))

(defn create-page [output-file title master-toc local-toc page-content]
  (with-open [out  (writer (file output-directory output-file))] 
    (binding [*out* out]
      (print
       (apply str (page title master-toc local-toc page-content))))))

(defsnippet make-master-toc master-toc-file
  [root]
  [project-info]
  [:div.ProjectTOC] #(at %
                         [:ul#left-sidebar-list :li]
                         (clone-for [project (for [[p d] project-info :when d] p)]
                                    (fn [n]
                                      (at n
                                          [:a] (do->
                                                (set-attr
                                                 :href
                                                 (str "http://clojure.github.com/" project "/"))
                                                (content project)))))))

(defn vars-by-letter 
  "Produce a lazy seq of two-vectors containing the letters A-Z and Other with all the 
vars in project-info that begin with that letter"
  [vars]
  (let [chars (conj (into [] (map #(str (char (+ 65 %))) (range 26))) "Other")
        var-map (apply merge-with conj 
                       (into {} (for [c chars] [c []]))
                       (for [v vars]
                         {(or (re-find #"[A-Z]" (-> v :name .toUpperCase))
                              "Other")
                          v}))]
    (for [c chars] [c (sort-by #(-> % :name .toUpperCase) (get var-map c))])))

(defn doc-prefix [v n]
  "Get a prefix of the doc string suitable for use in an index"
  (let [doc (:doc v)
        len (min (count doc) n)
        suffix (if (< len (count doc)) "..." ".")]
    (str (.replaceAll 
          (.replaceFirst (.substring doc 0 len) "^[ \n]*" "")
          "\n *" " ")
         suffix)))

(defn gen-index-line [v]
  (let [var-name (:name v)
        ns-name (:namespace v)
        overhead (count var-name)
        doc-len (+ 50 (min 0 (- 18 (count ns-name))))]
    #(at %
         [:a] (do->
               (set-attr :href
                         (str "http://clojure.github.com/" (:project v) "/"
                              (if (:one-namespace? v) "index.html" (str ns-name "-api.html"))
                              "#" ns-name "/" (:name v)))
               (content (:name v)))
         [:#line-content] (content 
                           (cl-format nil "~vt~a~vt~a~vt~a~%"
                                      (- 29 overhead)
                                      (:var-type v) (- 43 overhead)
                                      (:project v) (- 62 overhead)
                                      (doc-prefix v doc-len))))))

;; TODO: skip entries for letters with no members
(defsnippet make-index-content index-html-file-template [root]
  [vars-by-letter]
  [:div#index-body] (clone-for [[letter vars] vars-by-letter]
                               #(at %
                                    [:h2] (set-attr :id letter)
                                    [:span#section-head] (content letter)
                                    [:span#section-content] (clone-for [v vars]
                                                                       (gen-index-line v)))))

(defn make-index-html [vars master-toc]
  (create-page index-html-file
               "Index of the Clojure API"
               master-toc
               nil
               (make-index-content (vars-by-letter vars))))

(defn make-all-pages 
  ([project-info]
     (let [master-toc (make-master-toc project-info)]
       ;(make-overview ns-info master-toc)
       (make-index-html (flatten-vars project-info) master-toc))))

