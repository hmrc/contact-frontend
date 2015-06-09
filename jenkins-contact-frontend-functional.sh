#!/bin/bash

echo "Running functional test for the Contact Frontend..."

cd $WORKSPACE

echo "Starting ASSETS"

shopt -s expand_aliases
alias smc="sm --config /etc/smserver/conf"

smc --stop ALL
smc --cleanlogs

smc --start ASSETS_FRONTEND -r --wait 60 --noprogress

echo "Starting Contact Frontend"

sbt stage
target/universal/stage/bin/contact-frontend -Drun.mode=Stub &

while netstat -lnt | awk '$4 ~ /:9000$/ {exit 1}'; do sleep 5; done

sbt fun:test

kill -9 `lsof -t -i:9000`

smc --stop ALL

