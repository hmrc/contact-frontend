#!/bin/bash -e
BROWSER="remote-chrome"
ENV="local"

# port mapping for running contact-frontend service
port_mappings="6001->6001"

IMAGE=artefacts.tax.service.gov.uk/chrome-with-rinetd:latest

# When using on a Linux OS, add "--net=host" to the docker run command.
docker pull ${IMAGE} \
  && docker run \
  -d \
  --rm \
  --name "remote-chrome" \
  --shm-size=2g \
  -p 4444:4444 \
  -p 5900:5900 \
  -e PORT_MAPPINGS="$port_mappings" \
  -e TARGET_IP='host.docker.internal' \
  ${IMAGE}

sbt \
  -Dbrowser=$BROWSER \
  -Denvironment=$ENV \
  acceptance:test

