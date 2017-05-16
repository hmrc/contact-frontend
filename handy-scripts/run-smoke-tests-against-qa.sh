#!/bin/bash

# For some reason, this project will nut run with Chrome...
GECKODRIVER=`which geckodriver`

sbt -Dbrowser=firefox -Dwebdriver.gecko.driver=$GECKODRIVER smoke:test
