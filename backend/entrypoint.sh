#!/usr/bin/env bash

echo "FROM entrypoint.sh"

watchexec -r --exts java,html,css "./mvnw compile exec:java"
# watchman -- trigger ./src/main reload-app '*.java' '*.html' '*.css' -- mvn compile exec:java
# tail -f /dev/null
