#!/usr/bin/env bash

# NOTE: We *do* check in generated code, to mitigate different people using different versions of protoc. (Maybe this
# isn't really necessary, but it's what I think for now.)

set -u
set -e
set -o pipefail

PROTOC_PATH="$1"

"$PROTOC_PATH" \
    --proto_path="server/src/main/protobuf" \
    --java_out="server/src/main/java/com/haberdashervcs/server/protobuf" \
    folders.proto
