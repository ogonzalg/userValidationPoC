#!/bin/bash
#
# create crypto-config configuration
bin/cryptogen generate --config="crypto-config.yaml" --output="crypto-config"
rm src/test/resources/crypto-config -rf
cp crypto-config src/test/resources -r

# create orderer genesys block
bin/generateOrdererGenesisBlock.sh

# create channel configuration file
bin/generateChannelConf.sh
rm src/test/resources/config -rf
mkdir src/test/resources/config
cp channel-artifacts/channel.tx src/test/resources/config/channel.tx -f

# create final docker-compose.yaml with current private key files
bin/replacePrivateKey.sh

# start the network
docker-compose up -d