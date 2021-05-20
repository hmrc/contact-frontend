#!/bin/bash -e
ENV=${1:-local}
BROWSER=${2:-chrome}

sbt \
  -Dbrowser=$BROWSER \
  -Denvironment=$ENV \
  -Dwebdriver.chrome.driver=/usr/local/bin/chromedriver \
  -Dwebdriver.gecko.driver=/usr/local/bin/geckodriver \
  acceptance:test

