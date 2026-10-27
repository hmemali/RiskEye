package com.org.pfe.transaction_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

        @Value("${auth.service.api-key:}")
        private String authServiceApiKey;

    public UserContactResponse getUser(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", authServiceApiKey);
            return restTemplate.exchange(
                "http://authen-service/api/internal/users/" + userId + "/contact",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                UserContactResponse.class
            ).getBody();
        } catch (Exception exception) {
            return null;
        }
    }
}