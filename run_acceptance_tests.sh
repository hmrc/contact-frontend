#!/bin/bash -ex

BROWSER=$1
ENVIRONMENT=$2

sbt -Dbrowser="${BROWSER:=chrome}" -Denvironment="${ENVIRONMENT:=local}" "acceptance:test" testReport
