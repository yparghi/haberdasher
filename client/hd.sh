#!/usr/bin/env bash

set -u
set -e
set -x

BASE=$(dirname $0)

(cd "$BASE/../common" && mvn install)
(cd "$BASE/../client" && mvn install)

ARGS=$(echo "$@")
mvn -f "$BASE/pom.xml" exec:java \
    -Dexec.mainClass="com.haberdashervcs.client.ClientMain" \
    -Dexec.args="$ARGS"

