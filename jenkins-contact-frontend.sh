#!/bin/bash

cd $WORKSPACE
rm -rf service-manager-config
git clone git@github.tools.tax.service.gov.uk:HMRC/service-manager-config.git

echo "Starting ASSETS"

sm --stop ALL
sm --cleanlogs
sm --start ASSETS_FRONTEND -r --wait 60 --noprogress

echo "Running functional test for contact-frontend..."

cd $WORKSPACE

echo "Start functional tests..."

sbt clean test fun:test dist publish

echo "Gracefully shutdown server..."

sm --stop ALL
