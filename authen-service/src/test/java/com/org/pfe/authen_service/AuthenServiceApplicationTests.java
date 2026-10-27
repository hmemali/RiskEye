package com.org.pfe.authen_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthenServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void loginAllowsAnonymousRequestsAndReturnsUnauthorizedForUnknownCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"diagnostic@example.invalid\",\"password\":\"invalid-password\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void loginEmailIsCaseInsensitive() throws Exception {
		String email = "case-" + UUID.randomUUID() + "@example.test";
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fullName\":\"Case Test\",\"email\":\"" + email + "\",\"password\":\"valid-password\"}"))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email.toUpperCase(java.util.Locale.ROOT)
						+ "\",\"password\":\"valid-password\"}"))
			.andExpect(status().isOk());
	}

}
