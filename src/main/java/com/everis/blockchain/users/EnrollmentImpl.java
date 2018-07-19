package com.everis.blockchain.users;

import java.io.Serializable;
import java.security.PrivateKey;

import org.hyperledger.fabric.sdk.Enrollment;

public class EnrollmentImpl implements Enrollment, Serializable {
	private static final long serialVersionUID = -7067090120284213017L;
	private PrivateKey key;
	private String cert;

	public EnrollmentImpl(PrivateKey pkey, String signedPem) {
		this.key = pkey;
		this.cert = signedPem;
	}

	public PrivateKey getKey() {
		return key;
	}

	public String getCert() {
		return cert;
	}

}
