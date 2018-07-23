package com.everis.blockchain.poc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import com.everis.blockchain.users.EnrollmentImpl;
import com.everis.blockchain.users.UserImpl;
import com.everis.blockchain.util.CryptoFilesUtils;

public class Helper {
	
	private Helper(){
		//Avoid instantation
	}

	public static Orderer getOrderer(Properties conf, HFClient hfClient) throws InvalidArgumentException, URISyntaxException{
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
	
	public static Peer getPeer(Properties conf, HFClient hfClient, String org, String peer) throws URISyntaxException, InvalidArgumentException{
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
	
	public static User getAdmin(Properties conf, String org) throws URISyntaxException, InvalidKeySpecException, NoSuchAlgorithmException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, CryptoException, InvalidArgumentException, NoSuchMethodException, InvocationTargetException{
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
	



}
