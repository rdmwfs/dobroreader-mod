package org.anonymous.dobrochan.json;

public class DobroSession {
	private String language;
	private String __class__;
	private DobroToken[] tokens;
//	private Notification[] notifications;
	private DobroThread[] threads;
	private String password;
	private String id;
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String get__class__() {
		return __class__;
	}
	public void set__class__(String __class__) {
		this.__class__ = __class__;
	}
	public DobroToken[] getTokens() {
		return tokens;
	}
	public void setTokens(DobroToken[] tokens) {
		this.tokens = tokens;
	}
	public DobroThread[] getThreads() {
		return threads;
	}
	public void setThreads(DobroThread[] threads) {
		this.threads = threads;
	}
	/*
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	*/
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
}
