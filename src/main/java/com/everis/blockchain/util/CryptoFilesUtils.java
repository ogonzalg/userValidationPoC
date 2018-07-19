package com.everis.blockchain.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.hyperledger.fabric.sdk.security.CryptoSuite;

public class CryptoFilesUtils {
	public static PrivateKey readPrivateKeyPemFile(Path pkPath) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException{
		CryptoSuite.Factory.getCryptoSuite();

		PrivateKey key = null;
		
		StringBuilder keyBuilder = new StringBuilder();

		List<String> lines = Files.readAllLines(pkPath);
		
		for(String line: lines){
			if(line.indexOf("PRIVATE") == -1){
				keyBuilder.append(line);
			};
		}

		byte[] encoded = DatatypeConverter.parseBase64Binary(keyBuilder.toString());
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		KeyFactory kf = KeyFactory.getInstance("ECDSA");
		key = kf.generatePrivate(keySpec);

		return key;
	}
}
