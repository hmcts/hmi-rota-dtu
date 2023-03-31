package uk.gov.hmcts.reform.hmi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class WebClientCreationTest {

    @Test
    void createWebClientInsecure() {

        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        WebClient webClient =
            webClientConfiguration.webClientInsecure();
        assertNotNull(webClient, "WebClient has not been created successfully");
    }
}
