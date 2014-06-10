#!/bin/bash

lein run
status=$?

if [ ${status} -ne 0 ]
then
    echo "Index builder exited with status: ${status}, aborting..."
    exit 1
fi

(cd ../autodoc-work-area/clojure.github.com; git add .; git commit -m"Automated index build"; git push origin master)
