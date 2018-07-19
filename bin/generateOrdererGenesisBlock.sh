#!/bin/bash
#

export PATH=${PWD}/bin:${PWD}:$PATH
export FABRIC_CFG_PATH=${PWD}

function generateOrdererGenesisBlock() {
  which configtxgen
  if [ "$?" -ne 0 ]; then
    echo "configtxgen tool not found. exiting"
    exit 1
  fi

  mkdir -p channel-artifacts

  echo "###########################################################"
  echo "#########  Generating Orderer Genesis block  ##############"
  echo "###########################################################"
  PROFILE=Ejemplo1OrdererGenesis

  set -x
  configtxgen -profile $PROFILE -outputBlock ./channel-artifacts/genesis.block
  res=$?
  set +x
  if [ $res -ne 0 ]; then
    echo "Failed to generate orderer genesis block..."
    exit 1
  fi
}

generateOrdererGenesisBlock
