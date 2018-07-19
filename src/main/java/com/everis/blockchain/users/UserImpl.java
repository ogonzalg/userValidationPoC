package com.everis.blockchain.users;

import java.io.Serializable;
import java.util.Set;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

public class UserImpl implements User, Serializable {
	private static final long serialVersionUID = -3334198894583637345L;
	
	private String name;
	private Set<String> roles;
	private String account;
	private String affiliation;
	private Enrollment enrollment;
	private String mspId;

	public String getName() {
		return name;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public String getAccount() {
		return account;
	}

	public String getAffiliation() {
		return affiliation;
	}

	public Enrollment getEnrollment() {
		return enrollment;
	}

	public String getMspId() {
		return mspId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}

	public void setEnrollment(Enrollment enrollment) {
		this.enrollment = enrollment;
	}

	public void setMspId(String mspId) {
		this.mspId = mspId;
	}

}
