#!/bin/bash
#
DOCKER_HOST_IP=192.168.58.102

mkdir /tmp/local_m2
docker network create maven-net
docker run -it --rm --network maven-net --add-host ca.pagos.everis.com:$DOCKER_HOST_IP --add-host orderer.everis.com:$DOCKER_HOST_IP --add-host peer0.pagos.everis.com:$DOCKER_HOST_IP --add-host peer1.pagos.everis.com:$DOCKER_HOST_IP --add-host peer0.cobros.everis.com:$DOCKER_HOST_IP --add-host peer1.cobros.everis.com:$DOCKER_HOST_IP -v "$PWD":/usr/src/codigo -v /tmp/local_m2:/root/.m2 -w /usr/src/codigo maven mvn clean test
docker run -it --rm --network maven-net -v "$PWD":/usr/src/codigo -v /tmp/local_m2:/root/.m2 -w /usr/src/codigo maven mvn clean
#rm /tmp/local_m2 -rf
docker network rm maven-net

