#!/usr/bin/env bash

set -u
set -e
set -x

BASE=$(dirname $0)

# TODO: Figure out how to do this in hd.sh with an arg, this approach is dumb.
export NO_BUILD="true"
source "$BASE/hd.sh"

