package com.everis.blockchain.poc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.everis.blockchain.users.EnrollmentImpl;
import com.everis.blockchain.users.UserImpl;
import com.everis.blockchain.util.CryptoFilesUtils;

import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientValidationTest {
	private static User org1Admin;
	private static User org2Admin;
	
	private static final HFClient hfClient = HFClient.createNewInstance();
	private static HFCAClient hfcaClient;
	private static final Properties conf = new Properties();

	
	@BeforeClass
	public static void init() throws IOException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, InvalidArgumentException{
		conf.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
		
		hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

		org1Admin = getAdmin(conf, "org1");
		org2Admin = getAdmin(conf, "org2");
		
		Properties props = new Properties();
		props.setProperty("allowAllHostNames", "true");
		props.setProperty("pemFile", conf.getProperty("org1.ca.cert"));
		hfcaClient = HFCAClient.createNewInstance( conf.getProperty("org1.ca.url"), props);
		hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
	}

	@Test
	public void allTest() throws Exception {

		hfClient.setUserContext(org1Admin);
		createChannel();
		
		joinPeer("org1","peer0");
		joinPeer("org1","peer1");
		
		hfClient.setUserContext(org2Admin);
		joinPeer("org2","peer0");
		joinPeer("org2","peer1");

		deployChaincode();
		
		instantiateChainCode("init");

		registerSimpleUser();
		registerPowerUser();

	}
	
	private void deployChaincode() throws InvalidArgumentException, URISyntaxException, TransactionException, ProposalException, IOException {
		hfClient.setUserContext(org1Admin);

		List<Peer> org1Peers = new ArrayList<Peer>();
		org1Peers.add(getPeer("org1", "peer0"));
		org1Peers.add(getPeer("org1", "peer1"));
		
		for (ProposalResponse res : deployChainCode(org1Peers)) {
			System.out.println("- Chain code deployment " + res.getStatus());
		}


		hfClient.setUserContext(org2Admin);
		List<Peer> org2Peers = new ArrayList<Peer>();
		org2Peers.add(getPeer("org2", "peer0"));
		org2Peers.add(getPeer("org2", "peer1"));
		
		for (ProposalResponse res : deployChainCode(org2Peers)) {
			System.out.println("- Chain code deployment " + res.getStatus());
		}
		
	}

	private void instantiateChainCode(String functionName) throws InvalidArgumentException, ProposalException, URISyntaxException, TransactionException {
		hfClient.setUserContext(org1Admin);

		Channel channel = hfClient.newChannel(conf.getProperty("channel.name"));
		channel.addOrderer(getOrderer());
		channel.addPeer(getPeer("org1", "peer0"));
		channel.addPeer(getPeer("org1", "peer1"));
		channel.addPeer(getPeer("org2", "peer0"));
		channel.addPeer(getPeer("org2", "peer1"));
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
		CompletableFuture<TransactionEvent> cf = channel.sendTransaction(responses);
		
		for (ProposalResponse res : responses){
			System.out.println("- Chain code instantiation " + res.getStatus());
		}
	}

	private void createChannel() throws InvalidArgumentException, URISyntaxException, IOException, TransactionException {

		Orderer orderer = getOrderer();

		Path channelConfigPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty("channel.config.path")).toURI());
		ChannelConfiguration channelConfiguration = new ChannelConfiguration(channelConfigPath.toFile());
		byte[] channelConfigurationSignatures = hfClient.getChannelConfigurationSignature(channelConfiguration, org1Admin);

		Channel channel = hfClient.newChannel(conf.getProperty("channel.name"), orderer, channelConfiguration, channelConfigurationSignatures);
		System.out.println("- Channel  " + channel.getName() + " created");
	}
	
	private void joinPeer(String org, String peer) throws ProposalException, InvalidArgumentException, URISyntaxException, TransactionException{
		Channel channel = hfClient.newChannel(conf.getProperty("channel.name"));
		channel.addOrderer(getOrderer());
		channel.initialize();

		channel.joinPeer(getPeer(org,peer));
		System.out.println("- Peer " + org + "_" + peer + " joined to " + channel.getName());

	}

	private Orderer getOrderer() throws InvalidArgumentException, URISyntaxException{
		if(conf.getProperty("orderer.tsl.cert")!=null) {
			Path ordererTslCertPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty("orderer.tsl.cert")).toURI());
			Properties ordererProps = new Properties();
			ordererProps.setProperty("pemFile", ordererTslCertPath.toString());
			ordererProps.setProperty("sslProvider", "openSSL");
			ordererProps.setProperty("negotiationType", "TLS");
			
			return hfClient.newOrderer(conf.getProperty("orderer.name"), conf.getProperty("orderer.url"), ordererProps);
		} else {
			//TODO: Add non TLS orderer
			return null;
		}
	}
	
	private Peer getPeer(String org, String peer) throws URISyntaxException, InvalidArgumentException{
		if(conf.getProperty(org+"."+peer+"."+"tsl.cert")!=null){
			Path peerTslCertPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty(org+"."+peer+"."+"tsl.cert")).toURI());
			Properties peerProps = new Properties();
			peerProps.setProperty("pemFile", peerTslCertPath.toString());
			peerProps.setProperty("sslProvider", "openSSL");
			peerProps.setProperty("negotiationType", "TLS");
			return hfClient.newPeer(conf.getProperty(org + "." + peer +".name"), conf.getProperty(org + "." + peer +".url"), peerProps);
		} else {
			//TODO: Add non TLS peers
			return null;
		}
	}

	private static User getAdmin(Properties conf, String org) throws URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException, IOException{
		UserImpl orgAdmin = new UserImpl();
		
        Path pkPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty(org + ".admin.pk.path")).toURI());
		File pkFolder1 = pkPath.toFile();
		File[] pkFiles1 = pkFolder1.listFiles();
		PrivateKey pk = CryptoFilesUtils.readPrivateKeyPemFile(pkPath.resolve(pkFiles1[0].getName()));

        Path certPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty(org + ".admin.cert.path")).toURI());
		File certFolder = certPath.toFile();
		File[] certFiles = certFolder.listFiles();
		String certificate = new String(Files.readAllBytes(certPath.resolve(certFiles[0].getName())));

		Enrollment enrollOrg1Admin = new EnrollmentImpl(pk, certificate);
		orgAdmin.setEnrollment(enrollOrg1Admin);
		orgAdmin.setMspId(conf.getProperty(org + ".msp"));
		orgAdmin.setName(conf.getProperty(org + ".admin.name"));
		
		return orgAdmin;
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

	private User registerSimpleUser()  throws Exception {
		
		Enrollment adminEnrollment = hfcaClient.enroll(conf.getProperty("org1.admin.name"), conf.getProperty("org1.admin.password"));
		UserImpl adminUserContext = new UserImpl();
		adminUserContext.setName(conf.getProperty("org1.admin.name"));
		adminUserContext.setAffiliation(conf.getProperty("org1.name"));
		adminUserContext.setMspId(conf.getProperty("org1.msp"));
		adminUserContext.setEnrollment(adminEnrollment);

		// Register and Enroll user to Org1MSP
		UserImpl userContext = new UserImpl();
		String name = "user"+System.currentTimeMillis();
		userContext.setName(name);
		userContext.setAffiliation(conf.getProperty("org1.name"));
		userContext.setMspId(conf.getProperty("org1.msp"));

		RegistrationRequest request = new RegistrationRequest(userContext.getName(), userContext.getAffiliation());
		String password = hfcaClient.register(request, adminUserContext);
		Enrollment userEnrollment = hfcaClient.enroll(name, password);
		userContext.setEnrollment(userEnrollment);

		System.out.println("User " + name + " registered and enrolled");

		return userContext;
	}

	private User registerPowerUser()  throws Exception {
		
		Enrollment adminEnrollment = hfcaClient.enroll(conf.getProperty("org1.admin.name"), conf.getProperty("org1.admin.password"));
		UserImpl adminUserContext = new UserImpl();
		adminUserContext.setName(conf.getProperty("org1.admin.name"));
		adminUserContext.setAffiliation(conf.getProperty("org1.name"));
		adminUserContext.setMspId(conf.getProperty("org1.msp"));
		adminUserContext.setEnrollment(adminEnrollment);

		// Register and Enroll user to Org1MSP
		UserImpl userContext = new UserImpl();
		String name = "user"+System.currentTimeMillis();
		userContext.setName(name);
		userContext.setAffiliation(conf.getProperty("org1.name"));
		userContext.setMspId(conf.getProperty("org1.msp"));
		Set<String> roles = new HashSet<String>();
		roles.add("power");
		userContext.setRoles(roles);

		RegistrationRequest request = new RegistrationRequest(userContext.getName(), userContext.getAffiliation());
		String password = hfcaClient.register(request, adminUserContext);
		Enrollment userEnrollment = hfcaClient.enroll(name, password);
		userContext.setEnrollment(userEnrollment);

		System.out.println("User " + name + " registered and enrolled");

		return userContext;
	}

}
