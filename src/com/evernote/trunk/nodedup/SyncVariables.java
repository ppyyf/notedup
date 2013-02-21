package com.evernote.trunk.nodedup;

public class SyncVariables {
	public static final String VERIFIER_NOT_SET = "NOTSET"; //$NON-NLS-1$
	private static String verifier = VERIFIER_NOT_SET;
	public synchronized static String getVerifier(){
		return verifier;
	}
	public synchronized static void setVerifier(String v){
		verifier = v;
	}
	public synchronized static void resetVerifier(){
		verifier = VERIFIER_NOT_SET;
	}
}
