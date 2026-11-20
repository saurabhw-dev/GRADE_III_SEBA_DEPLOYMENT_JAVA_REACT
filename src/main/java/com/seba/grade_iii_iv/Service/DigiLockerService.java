package com.seba.grade_iii_iv.Service;

import java.util.Map;

public interface DigiLockerService {
	
	public String generateRandomString(int length);
	public String generateCodeChallenge(String verifier) throws Exception;
	public Map<String, String> generateAuthorization() throws Exception;
	public Map<String, Object> generateAccessToken(String code,String verifier);
	public Map<String, Object> getUserDetails(String accessToken);
	public Map<String, Object> getIssuedDocuments(String accessToken);
	public Map<String, Object> getEaadhaarDetails(String accessToken) throws Exception;

}
