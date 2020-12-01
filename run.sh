#!/usr/bin/env bash
sbt \
  -Dconfig.resource=local.conf \
  -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes \
  run
