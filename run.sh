#!/bin/bash 

lein run
(cd ../autodoc-work-area/clojure.github.io; git add .; git commit -m"Automated index build"; git push origin master)
