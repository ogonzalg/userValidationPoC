package com.everis.blockchain.poc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.BeforeClass;
import org.junit.Test;

public class DeployInstantiateChainCodeTest {
	private static User org1Admin;
	private static User org2Admin;
	
	private static final HFClient hfClient = HFClient.createNewInstance();
	private static final Properties conf = new Properties();
	
	@BeforeClass
	public static void init() throws IOException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, InvalidArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException{
		java.security.Security.addProvider(new BouncyCastleProvider());

		conf.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
		
		hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

		org1Admin = Helper.getAdmin(conf,"org1");
		org2Admin = Helper.getAdmin(conf,"org2");
	}

	@Test
	public void allTest() throws Exception {

		deployChaincode();
		
		instantiateChainCode("init");
	}

	private void deployChaincode() throws InvalidArgumentException, URISyntaxException, TransactionException, ProposalException, IOException {
		hfClient.setUserContext(org1Admin);

		List<Peer> org1Peers = new ArrayList<Peer>();
		org1Peers.add(Helper.getPeer(conf, hfClient, "org1", "peer0"));
		org1Peers.add(Helper.getPeer(conf, hfClient, "org1", "peer1"));
		
		for (ProposalResponse res : deployChainCode(org1Peers)) {
			System.out.println("- Chain code deployment " + res.getStatus());
		}


		hfClient.setUserContext(org2Admin);
		List<Peer> org2Peers = new ArrayList<Peer>();
		org2Peers.add(Helper.getPeer(conf, hfClient, "org2", "peer0"));
		org2Peers.add(Helper.getPeer(conf, hfClient, "org2", "peer1"));
		
		for (ProposalResponse res : deployChainCode(org2Peers)) {
			System.out.println("- Chain code deployment " + res.getStatus());
		}
		
	}

	private Collection<ProposalResponse> deployChainCode(Collection<Peer> peers)
			throws InvalidArgumentException, IOException, ProposalException, URISyntaxException {
		InstallProposalRequest request = hfClient.newInstallProposalRequest();
		ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(conf.getProperty("chaincode.name")).setVersion(conf.getProperty("chaincode.version")).setPath(conf.getProperty("chaincode.path"));
		ChaincodeID chaincodeID = chaincodeIDBuilder.build();

		request.setChaincodeID(chaincodeID);
		request.setUserContext(hfClient.getUserContext());
		
		Path codePath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty("chaincode.rootdir")).toURI());
		request.setChaincodeSourceLocation(codePath.toFile());
		request.setChaincodeVersion(conf.getProperty("chaincode.version"));
		Collection<ProposalResponse> responses = hfClient.sendInstallProposal(request, peers);
		return responses;
	}

	private void instantiateChainCode(String functionName) throws InvalidArgumentException, ProposalException, URISyntaxException, TransactionException {
		hfClient.setUserContext(org1Admin);

		Channel channel = hfClient.newChannel(conf.getProperty("channel.name"));
		channel.addOrderer(Helper.getOrderer(conf, hfClient));
		channel.addPeer(Helper.getPeer(conf, hfClient, "org1", "peer0"));
		channel.addPeer(Helper.getPeer(conf, hfClient, "org1", "peer1"));
		channel.addPeer(Helper.getPeer(conf, hfClient, "org2", "peer0"));
		channel.addPeer(Helper.getPeer(conf, hfClient, "org2", "peer1"));
		channel.initialize();
		
		InstantiateProposalRequest instantiateProposalRequest = hfClient.newInstantiationProposalRequest();
		instantiateProposalRequest.setProposalWaitTime(180000);
		ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(conf.getProperty("chaincode.name")).setVersion(conf.getProperty("chaincode.version")).setPath(conf.getProperty("chaincode.path"));
		ChaincodeID ccid = chaincodeIDBuilder.build();

		instantiateProposalRequest.setChaincodeID(ccid);
		instantiateProposalRequest.setChaincodeLanguage(Type.GO_LANG);

		instantiateProposalRequest.setFcn(functionName);
		instantiateProposalRequest.setArgs(new String[] { "" });
		Map<String, byte[]> tm = new HashMap<>();
		tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
		tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
		instantiateProposalRequest.setTransientMap(tm);

		Collection<ProposalResponse> responses = channel.sendInstantiationProposal(instantiateProposalRequest);
		channel.sendTransaction(responses);
		
		for (ProposalResponse res : responses){
			System.out.println("- Chain code instantiation " + res.getStatus());
		}
	}

	
}
