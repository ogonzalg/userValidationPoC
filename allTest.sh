#!/bin/bash
#
./cleanAll.sh
./configureNetwork.sh
mvn clean test
mvn clean
./cleanAll.sh

