#!/bin/bash -e
BROWSER="remote-chrome"
ENV="local"

# sm2 ports needed when running assets-frontend locally via service manager
port_mappings=$(sm2 --status | grep PASS | awk '{ print $8"->"$8 }' | paste -sd "," -)

# port mapping for running contact-frontend service
service_port_mapping="6001->6001,"
port_mappings="$service_port_mapping$sm_port_mapping"

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

