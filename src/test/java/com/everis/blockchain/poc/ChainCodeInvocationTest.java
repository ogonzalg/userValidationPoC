package com.everis.blockchain.poc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.junit.BeforeClass;
import org.junit.Test;

import com.everis.blockchain.users.UserImpl;

public class ChainCodeInvocationTest {

	private static final HFClient hfClient = HFClient.createNewInstance();
	private static HFCAClient hfcaClient;
	private static final Properties conf = new Properties();

	@BeforeClass
	public static void init() throws IOException, URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException, CryptoException, InvalidArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException{
		java.security.Security.addProvider(new BouncyCastleProvider());

		conf.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
		
		hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

		Properties props = new Properties();
		props.setProperty("allowAllHostNames", "true");
		Path caCertPath = Paths.get(Thread.currentThread().getContextClassLoader().getResource(conf.getProperty("org1.ca.cert")).toURI());
		props.setProperty("pemFile", caCertPath.toString());
		hfcaClient = HFCAClient.createNewInstance( conf.getProperty("org1.ca.url"), props);
		hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
	}

	@Test
	public void chainCodeInvocationTest() throws Exception {
		Set<Attribute> attributes = new HashSet<Attribute>();
		
		attributes.add(new Attribute("powered", "no",true));
		attributes.add(new Attribute("role", "Blue"));
		HFCAIdentity simple = registerUser(attributes);
		
		attributes.clear();
		attributes.add(new Attribute("powered", "yes",true));
		attributes.add(new Attribute("role", "Red"));
		HFCAIdentity powered = registerUser(attributes);

		invoqueChainCode(simple);
		invoqueChainCode(powered);
		
		queryChainCode(simple);
	}
	
	private HFCAIdentity registerUser(Collection<Attribute> attributes)  throws Exception {
		
		Enrollment adminEnrollment = hfcaClient.enroll(conf.getProperty("org1.admin.name"), conf.getProperty("org1.admin.password"));
		UserImpl adminUserContext = new UserImpl();
		adminUserContext.setName(conf.getProperty("org1.admin.name"));
		adminUserContext.setAffiliation(conf.getProperty("org1.name"));
		adminUserContext.setMspId(conf.getProperty("org1.msp"));
		adminUserContext.setEnrollment(adminEnrollment);
		
		String name = "user"+System.currentTimeMillis();
		HFCAIdentity identity = hfcaClient.newHFCAIdentity(name);
		identity.setAffiliation(conf.getProperty("org1.name"));
		identity.getAttributes().addAll(attributes);
		identity.setSecret("userPassword");
		identity.create(adminUserContext);
		
		Enrollment userEnrollment = hfcaClient.enroll(name, "userPassword");

		System.out.println("User " + name + " registered and enrolled");
		System.out.println("User cert:");
		System.out.println(userEnrollment.getCert());
		
		return identity;
	}

	private void invoqueChainCode(HFCAIdentity user) throws InvalidArgumentException, URISyntaxException, TransactionException, ProposalException, EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
		UserImpl userContext = new UserImpl();
		userContext.setName(user.getEnrollmentId());
		userContext.setMspId(conf.getProperty("org1.msp"));
		userContext.setEnrollment(hfcaClient.enroll(user.getEnrollmentId(), user.getSecret()));
		userContext.setAffiliation(user.getAffiliation());

		hfClient.setUserContext(userContext);
		Channel channel = hfClient.newChannel(conf.getProperty("channel.name"));
		channel.addOrderer(Helper.getOrderer(conf, hfClient));
		channel.addPeer(Helper.getPeer(conf, hfClient,"org1", "peer0"));
		channel.initialize();
		
		TransactionProposalRequest request = hfClient.newTransactionProposalRequest();
		ChaincodeID ccid = ChaincodeID.newBuilder().setName(conf.getProperty("chaincode.name")).build();
		request.setChaincodeID(ccid);
		request.setFcn("initLedger");
		request.setProposalWaitTime(1000);
		
		request.setUserContext(userContext);
		
		Map<String, byte[]> tm2 = new HashMap<>();
		tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8)); 																								
		tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8)); 
		tm2.put("result", ":)".getBytes(UTF_8));
		tm2.put("event", "!".getBytes(UTF_8)); 
		request.setTransientMap(tm2);

		Collection<ProposalResponse> responses = channel.sendTransactionProposal(request, channel.getPeers());
		channel.sendTransaction(responses,userContext);
		for (ProposalResponse res: responses) {
			Status status = res.getStatus();
			System.out.println("Invoked createCar on "+ conf.getProperty("chaincode.name") + ". Status - " + status);
		}
		
		channel.shutdown(false);
	}

	private void queryChainCode(HFCAIdentity user) throws InvalidArgumentException, URISyntaxException, TransactionException, ProposalException, EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException {
		Channel channel = hfClient.newChannel(conf.getProperty("channel.name"));
		channel.addOrderer(Helper.getOrderer(conf, hfClient));
		channel.addPeer(Helper.getPeer(conf, hfClient,"org1", "peer0"));
		channel.initialize();
		
		QueryByChaincodeRequest request = hfClient.newQueryProposalRequest();
		ChaincodeID ccid = ChaincodeID.newBuilder().setName(conf.getProperty("chaincode.name")).build();
		request.setChaincodeID(ccid);
		request.setFcn("queryAllCars");
		request.setProposalWaitTime(1000);
		
		UserImpl userContext = new UserImpl();
		userContext.setName(user.getEnrollmentId());
		userContext.setMspId(conf.getProperty("org1.msp"));
		userContext.setEnrollment(hfcaClient.enroll(user.getEnrollmentId(), user.getSecret()));
		userContext.setAffiliation(user.getAffiliation());
		request.setUserContext(userContext);

		Collection<ProposalResponse> responses = channel.queryByChaincode(request);
		for (ProposalResponse pres : responses) {
			System.out.println(pres.getStatus());
			String stringResponse = new String(pres.getChaincodeActionResponsePayload());
			System.out.println(stringResponse);
		}
	}

}
