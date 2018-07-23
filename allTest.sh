#!/bin/bash
#
./cleanAll.sh
./configureNetwork.sh
./mavenChannelCreationTest.sh
./mavenDeployInstantiateChainCodeTest.sh
./mavenChainCodeInvocationTest.sh
./cleanAll.sh

