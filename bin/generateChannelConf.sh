#!/bin/bash
#

export PATH=${PWD}/../bin:${PWD}:$PATH
export FABRIC_CFG_PATH=${PWD}

echo "Config path: $FABRIC_CFG_PATH"

function generateOrdererGenesisBlock() {
  which configtxgen
  if [ "$?" -ne 0 ]; then
    echo "configtxgen tool not found. exiting"
    exit 1
  fi

  mkdir -p channel-artifacts

  echo
  echo "###################################################################"
  echo "###  Generating channel configuration transaction  'channel.tx' ###"
  echo "###################################################################"
  CHANNEL_PROFILE=EjemploChannel
  CHANNEL_NAME=ejemplochannel

  set -x
  configtxgen -profile $CHANNEL_PROFILE -outputCreateChannelTx channel-artifacts/channel.tx -channelID $CHANNEL_NAME
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate channel configuration transaction..."
    exit 1
  fi
}

generateOrdererGenesisBlock
