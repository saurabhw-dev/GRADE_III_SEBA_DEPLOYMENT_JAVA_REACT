package com.seba.grade_iii_iv.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DigiLockerServiceImplementation implements DigiLockerService {

	@Value("${digilocker.client-id}")
	private String clientId;

	@Value("${digilocker.client-secret}")
	private String clientSecret;

	@Value("${digilocker.redirect-uri}")
	private String redirectUri;

	@Value("${digilocker.authorize-url}")
	private String authorizeUrl;

	@Value("${digilocker.token-url}")
	private String tokenUrl;

	@Value("${digilocker.user-url}")
	private String userUrl;

	@Value("${digilocker.issued-url}")
	private String issuedUrl;

	@Value("${digilocker.scope}")
	private String scope;

	@Value("${digilocker.eaadhaar-url}")
	private String eaadhaarUrl;

	private final RestTemplate restTemplate;

	private final SecureRandom secureRandom = new SecureRandom();

	public DigiLockerServiceImplementation(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	// Generate secure random string
	public String generateRandomString(int length) {

		byte[] randomBytes = new byte[length];

		secureRandom.nextBytes(randomBytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	// Generate PKCE SHA256 challenge
	@Override
	public String generateCodeChallenge(String verifier) throws Exception {

		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));

		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}

	// Generate authorization URL
	public Map<String, String> generateAuthorization() throws Exception {

		String state = generateRandomString(32);

		String verifier = generateRandomString(64);

		String challenge = generateCodeChallenge(verifier);

		String url = UriComponentsBuilder.fromUriString(authorizeUrl).queryParam("response_type", "code")
				.queryParam("client_id", clientId).queryParam("redirect_uri", redirectUri).queryParam("state", state)
				.queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256")
				.queryParam("scope", scope).build().encode().toUriString();

		return Map.of("url", url, "state", state, "verifier", verifier);
	}

	// Exchange authorization code for access token
	public Map<String, Object> generateAccessToken(String code, String verifier) {

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

		body.add("code", code);
		body.add("grant_type", "authorization_code");
		body.add("client_id", clientId);
		body.add("client_secret", clientSecret);
		body.add("redirect_uri", redirectUri);
		body.add("code_verifier", verifier);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

		return restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class).getBody();
	}

	// Fetch DigiLocker user details
	public Map<String, Object> getUserDetails(String accessToken) {

		HttpHeaders headers = new HttpHeaders();

		headers.setBearerAuth(accessToken);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		return restTemplate.exchange(userUrl, HttpMethod.GET, request, Map.class).getBody();
	}

	// Fetch issued document list
	public Map<String, Object> getIssuedDocuments(String accessToken) {

		HttpHeaders headers = new HttpHeaders();

		headers.setBearerAuth(accessToken);

		HttpEntity<Void> request = new HttpEntity<>(headers);

		return restTemplate.exchange(issuedUrl, HttpMethod.GET, request, Map.class).getBody();
	}

	/*@Override
	public Map<String, Object> getEaadhaarDetails(String accessToken) {

		System.out.println("getEaadhaarDetails");
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(eaadhaarUrl, HttpMethod.GET, entity, String.class);

		Map<String, Object> result = new HashMap<>();

		result.put("xml", response.getBody());
		result.put("hmac", response.getHeaders().getFirst("hmac"));

		return result;
	}*/
	
	@Override
	public Map<String, Object> getEaadhaarDetails(String accessToken) {

	    System.out.println("getEaadhaarDetails called");

	    try {

	        if (accessToken == null || accessToken.isEmpty()) {
	            throw new RuntimeException("DigiLocker access token is missing.");
	        }

	        System.out.println("e-Aadhaar URL: " + eaadhaarUrl);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setBearerAuth(accessToken);
	        headers.setAccept(List.of(MediaType.APPLICATION_XML));

	        HttpEntity<String> entity = new HttpEntity<>(headers);

	        ResponseEntity<String> response = restTemplate.exchange(
	                eaadhaarUrl,
	                HttpMethod.GET,
	                entity,
	                String.class
	        );

	        Map<String, Object> result = new HashMap<>();

	        result.put("xml", response.getBody());
	        result.put("hmac", response.getHeaders().getFirst("hmac"));
	        result.put("status", response.getStatusCode().value());

	        return result;

	    } catch (HttpStatusCodeException e) {

	        System.out.println("DigiLocker HTTP Status: " + e.getStatusCode());
	        System.out.println("DigiLocker Error: " + e.getResponseBodyAsString());

	        throw e;

	    } catch (Exception e) {

	        e.printStackTrace();
	        throw new RuntimeException("e-Aadhaar request failed: " + e.getMessage(), e);
	    }
	}

}
