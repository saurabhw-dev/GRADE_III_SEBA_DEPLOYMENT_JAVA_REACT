package com.seba.grade_iii_iv.Controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.seba.grade_iii_iv.Service.DigiLockerService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/digilocker")
@CrossOrigin(origins = { "http://localhost:3000", "https://uat-seba.formsrec.in" }, allowCredentials = "true")
public class DigiLockerController {

	private final DigiLockerService digiLockerService;

	public DigiLockerController(DigiLockerService digiLockerService) {
		this.digiLockerService = digiLockerService;
	}

	// STEP 1: Start DigiLocker login
	@GetMapping("/authorize")
	public RedirectView authorize(HttpSession session) throws Exception {

		Map<String, String> authorization = digiLockerService.generateAuthorization();

		// Store state and verifier in backend session
		session.setAttribute("DIGILOCKER_STATE", authorization.get("state"));

		session.setAttribute("DIGILOCKER_VERIFIER", authorization.get("verifier"));

		// Redirect candidate to DigiLocker
		return new RedirectView(authorization.get("url"));
	}

	// STEP 2: Receive authorization code from React
	@PostMapping("/exchange")
	public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> request, HttpSession session) {

		try {

			String code = request.get("code");
			String receivedState = request.get("state");

			String savedState = (String) session.getAttribute("DIGILOCKER_STATE");

			String verifier = (String) session.getAttribute("DIGILOCKER_VERIFIER");

			if (code == null || receivedState == null || savedState == null || verifier == null
					|| !savedState.equals(receivedState)) {

				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("message", "Invalid DigiLocker authorization state"));
			}

			// Remove one-time state and verifier
			session.removeAttribute("DIGILOCKER_STATE");
			session.removeAttribute("DIGILOCKER_VERIFIER");

			// Exchange code for token
			Map<String, Object> tokenResponse = digiLockerService.generateAccessToken(code, verifier);

			if (tokenResponse == null || tokenResponse.get("access_token") == null) {

				return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
						.body(Map.of("message", "Unable to obtain DigiLocker access token"));
			}

			String accessToken = tokenResponse.get("access_token").toString();

			// Keep token on backend only
			session.setAttribute("DIGILOCKER_ACCESS_TOKEN", accessToken);

			// Fetch candidate profile
			Map<String, Object> profile = digiLockerService.getUserDetails(accessToken);

			if (profile == null) {
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
						.body(Map.of("message", "Unable to fetch DigiLocker profile"));
			}

			// Keep profile in backend session
			session.setAttribute("DIGILOCKER_PROFILE", profile);

			// Return profile, never return access token
			return ResponseEntity.ok(profile);

		} catch (Exception e) {

			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body(Map.of("message", "DigiLocker verification failed"));
		}
	}

	// STEP 3: Get already verified profile
	@GetMapping("/profile")
	public ResponseEntity<?> getProfile(HttpSession session) {

		Map<String, Object> profile = (Map<String, Object>) session.getAttribute("DIGILOCKER_PROFILE");

		if (profile == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "DigiLocker verification not completed"));
		}

		return ResponseEntity.ok(profile);
	}

	// STEP 4: Get issued document list
	@GetMapping("/issued-documents")
	public ResponseEntity<?> getIssuedDocuments(HttpSession session) {

		String accessToken = (String) session.getAttribute("DIGILOCKER_ACCESS_TOKEN");

		if (accessToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "Please authenticate with DigiLocker first"));
		}

		try {

			Map<String, Object> documents = digiLockerService.getIssuedDocuments(accessToken);

			return ResponseEntity.ok(documents);

		} catch (Exception e) {

			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
					.body(Map.of("message", "Unable to fetch issued documents"));
		}
	}

	@GetMapping("/eaadhaar")
	public ResponseEntity<?> getEaadhaar(HttpSession session) {

		String accessToken = (String) session.getAttribute("DIGILOCKER_ACCESS_TOKEN");

		if (accessToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please authenticate with DigiLocker first.");
		}

		try {
			Map<String, Object> result = digiLockerService.getEaadhaarDetails(accessToken);

			return ResponseEntity.ok(result);

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to retrieve e-Aadhaar.");
		}
	}

	@GetMapping("/driving-license")
	public ResponseEntity<?> getDrivingLicense(HttpSession session) {
		System.out.println("driving-license");
		String accessToken = (String) session.getAttribute("DIGILOCKER_ACCESS_TOKEN");

		if (accessToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please authenticate with DigiLocker first.");
		}

		try {

			Map<String, Object> result = digiLockerService.getIssuedDocuments(accessToken);

			return ResponseEntity.ok(result);

		} catch (Exception e) {

			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Unable to retrieve Driving Licence documents.");
		}
	}

	/*@GetMapping("/driving-license-xml")
	public ResponseEntity<?> getDrivingLicenseXml(HttpSession session) {
		System.out.println("driving-license-xml");
		String accessToken = (String) session.getAttribute("DIGILOCKER_ACCESS_TOKEN");

		if (accessToken == null) {

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please authenticate with DigiLocker first.");
		}

		try {

			Map<String, Object> result = digiLockerService.getDrivingLicenseXml(accessToken);

			return ResponseEntity.ok(result);

		} catch (Exception e) {

			e.printStackTrace();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}*/
	
	@GetMapping("/pan-card")
	public ResponseEntity<?> getPanCard(HttpSession session) {
		System.out.println("pan-card");
	    String accessToken = (String) session.getAttribute("DIGILOCKER_ACCESS_TOKEN");

	    if (accessToken == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body("DigiLocker session expired. Please login again.");
	    }

	    try {
	        Map<String, Object> response =
	                digiLockerService.getPanCardXml(accessToken);

	        return ResponseEntity.ok(response);

	    } catch (Exception e) {

	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(e.getMessage());
	    }
	}

	// STEP 5: Clear DigiLocker session data
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session) {

		session.removeAttribute("DIGILOCKER_ACCESS_TOKEN");
		session.removeAttribute("DIGILOCKER_PROFILE");
		session.removeAttribute("DIGILOCKER_STATE");
		session.removeAttribute("DIGILOCKER_VERIFIER");

		return ResponseEntity.ok(Map.of("message", "DigiLocker session cleared"));
	}

}
