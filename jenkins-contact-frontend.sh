#!/bin/bash

echo "Starting ASSETS"

shopt -s expand_aliases

alias smc="sm --config /etc/smserver/conf"
smc --stop ALL
smc --cleanlogs
smc --start ASSETS_FRONTEND -r --wait 60 --noprogress

echo "Running functional test for contact-frontend..."

cd $WORKSPACE

echo "Start functional tests..."

sbt clean test fun:test dist publish

echo "Gracefully shutdown server..."

smc --stop ALL
