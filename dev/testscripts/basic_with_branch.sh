#!/usr/bin/env bash

cd ~/src/hd-test-repos
mkdir -p basic-1
cd basic-1

rm -r *
~/src/haberdasher/client/hd.sh init localhost:15367 some_org some_repo
bash ~/src/haberdasher/client/hd_nobuild.sh checkout /

~/src/haberdasher/client/hd_nobuild.sh branch create some_branch
~/src/haberdasher/client/hd_nobuild.sh status

echo "new on branch" > subfolder/new.txt
~/src/haberdasher/client/hd_nobuild.sh commit
~/src/haberdasher/client/hd_nobuild.sh status

echo "

To push:
~/src/haberdasher/client/hd_nobuild.sh push
"

