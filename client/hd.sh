#!/usr/bin/env bash

set -e
set -x

BASE=$(dirname $0)
SKIPTESTS="-DskipTests"

if [[ -z "$NO_BUILD" ]]; then
    (cd "$BASE/../common" && mvn install "$SKIPTESTS")
    (cd "$BASE/../client" && mvn install "$SKIPTESTS")
fi

ARGS=$(echo "$@")
mvn -f "$BASE/pom.xml" exec:java \
    -Dexec.mainClass="com.haberdashervcs.client.ClientMain" \
    -Dexec.args="$ARGS" "$SKIPTESTS"

