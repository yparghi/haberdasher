#!/usr/bin/env bash

set -u
set -e
set -x

BASE=$(dirname $0)
SKIPTESTS="-DskipTests"

(cd "$BASE/../common" && mvn install "$SKIPTESTS")
(cd "$BASE/../client" && mvn install "$SKIPTESTS")

ARGS=$(echo "$@")
mvn -f "$BASE/pom.xml" exec:java \
    -Dexec.mainClass="com.haberdashervcs.client.ClientMain" \
    -Dexec.args="$ARGS" "$SKIPTESTS"

