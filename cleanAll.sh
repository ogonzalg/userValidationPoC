#!/bin/bash
#
# Clean crypto-config configuration
rm crypto-config -rf
rm src/test/resources/crypto-config -rf
# Clean channel configuration
rm src/test/resources/config -rf
rm channel-artifacts -rf
# stop and clean docker stuff
docker-compose down -v
# remove docker-compose yaml
rm docker-compose.yaml -f
