package com.testApp.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@SpringBootApplication
public class Application implements CommandLineRunner {

	@Value("${candidate.name}")
	private String candidateName;

	@Value("${candidate.regno}")
	private String candidateRegNo;

	@Value("${candidate.email}")
	private String candidateEmail;

	@Value("${final.query.string:}")
	private String finalQueryFromProps;

	@Value("${http.connect.timeout:8000}")
	private int connectTimeoutMs;

	@Value("${http.read.timeout:12000}")
	private int readTimeoutMs;

	@Autowired
	private RestTemplate restTemplate;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("=== Bajaj Finserv Health | Qualifier 1 | JAVA ===");
		System.out.println("RegNo: " + candidateRegNo + "  |  Name: " + candidateName);

		String questionType = isLastTwoDigitsOdd(candidateRegNo) ? "Q1 (ODD)" : "Q2 (EVEN)";
		System.out.println("Detected: " + questionType);

		if (isLastTwoDigitsOdd(candidateRegNo)) {
			System.out.println("Your SQL is Question 1.");
		} else {
			System.out.println("Your SQL is Question 2.");
		}

		String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
		Map<String, Object> generatePayload = new HashMap<>();
		generatePayload.put("name", candidateName);
		generatePayload.put("regNo", candidateRegNo);
		generatePayload.put("email", candidateEmail);

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(generatePayload, headers);

			System.out.println("Calling generateWebhook...");
			ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, request, Map.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				System.err.println("generateWebhook failed: " + response.getStatusCode());
				System.exit(2);
				return;
			}

			Map body = response.getBody();
			String accessToken = getString(body, "accessToken");
			String webhook = getString(body, "webhook");

			if (webhook == null || webhook.isEmpty()) {
				webhook = getString(body, "webhookUrl"); 
			}

			System.out.println("Received accessToken: " + (accessToken != null ? "[REDACTED]" : "null"));
			System.out.println("Received webhook: " + webhook);

			if (accessToken == null || accessToken.isEmpty()) {
				System.err.println("No accessToken returned. Cannot proceed.");
				System.exit(2);
				return;
			}

			String finalQuery = loadFinalQuery();
			if (finalQuery == null || finalQuery.trim().isEmpty()) {
				System.err.println("ERROR: final SQL query not found.");
				System.exit(3);
				return;
			}
			finalQuery = finalQuery.trim();

			String submitUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
			Map<String, Object> submitPayload = new HashMap<>();
			submitPayload.put("finalQuery", finalQuery);

			HttpHeaders submitHeaders = new HttpHeaders();
			submitHeaders.setContentType(MediaType.APPLICATION_JSON);
			submitHeaders.set("Authorization", accessToken);

			HttpEntity<Map<String, Object>> submitRequest =
					new HttpEntity<>(submitPayload, submitHeaders);

			System.out.println("Submitting final SQL to testWebhook...");
			ResponseEntity<String> submitResp =
					restTemplate.postForEntity(submitUrl, submitRequest, String.class);

			System.out.println("Submission HTTP Status: " + submitResp.getStatusCode());
			System.out.println("Submission Response Body:");
			System.out.println(submitResp.getBody());

			System.out.println("Done.");
			System.exit(0);

		} catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private String loadFinalQuery() {
		Path p = Paths.get("final_query.sql");
		if (Files.exists(p)) {
			try {
				return Files.readString(p, StandardCharsets.UTF_8);
			} catch (IOException e) {
				System.err.println("Could not read final_query.sql: " + e.getMessage());
			}
		}
		if (finalQueryFromProps != null && !finalQueryFromProps.trim().isEmpty()) {
			return finalQueryFromProps;
		}
		return null;
	}

	private boolean isLastTwoDigitsOdd(String regNo) {
		int len = regNo.length();
		int i = len - 1;
		StringBuilder sb = new StringBuilder();
		int count = 0;
		while (i >= 0 && count < 2) {
			char c = regNo.charAt(i);
			if (Character.isDigit(c)) {
				sb.append(c);
				count++;
			}
			i--;
		}
		if (sb.length() == 0) return false;
		String lastTwo = sb.reverse().toString();
		try {
			int val = Integer.parseInt(lastTwo);
			return (val % 2) == 1;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String getString(Map map, String key) {
		if (map == null) return null;
		Object v = map.get(key);
		if (v == null) return null;
		if (v instanceof String) return (String) v;
		return String.valueOf(v);
	}
}
