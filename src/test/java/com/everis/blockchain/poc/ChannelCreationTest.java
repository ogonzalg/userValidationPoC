package com.everis.blockchain.poc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChannelCreationTest {
	private static User org1Admin;
	private static User org2Admin;
	
	private static final HFClient hfClient = HFClient.createNewInstance();
	private static final Properties conf = new Properties();

	@BeforeClass
	public static void init() throws IOException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, InvalidArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException{
		java.security.Security.addProvider(new BouncyCastleProvider());

		conf.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
		
		hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

		org1Admin = Helper.getAdmin(conf, "org1");
		org2Admin = Helper.getAdmin(conf, "org2");
	}

	@Test
	public void channelCreationTest() throws Exception {

		hfClient.setUserContext(org1Admin);
		Channel channel = createChannel();
		channel.joinPeer(Helper.getPeer(conf, hfClient, "org1","peer0"));
		channel.joinPeer(Helper.getPeer(conf, hfClient, "org1","peer1"));
		
		hfClient.setUserContext(org2Admin);
		channel.joinPeer(Helper.getPeer(conf, hfClient, "org2","peer0"));
		channel.joinPeer(Helper.getPeer(conf, hfClient, "org2","peer1"));
	}

	private Channel createChannel() throws InvalidArgumentException, URISyntaxException, IOException, TransactionException {

		Orderer orderer = Helper.getOrderer(conf, hfClient);

		Path channelConfigPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty("channel.config.path")).toURI());
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelConfigPath.toFile());
		byte[] channelConfigurationSignatures = hfClient.getChannelConfigurationSignature(channelConfiguration, org1Admin);

		return hfClient.newChannel(conf.getProperty("channel.name"), orderer, channelConfiguration, channelConfigurationSignatures);
	}
	
}
