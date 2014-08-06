#!/bin/sh

SCRIPT=$(find . -type f -name contact-frontend)
exec $SCRIPT \
  $HMRC_CONFIG
