#!/bin/bash

# For some reason, this project will nut run with Chrome...
GECKODRIVER=`which geckodriver`

sbt -Denvironment=qa -Dbrowser=firefox -Dwebdriver.gecko.driver=$GECKODRIVER fun:test
