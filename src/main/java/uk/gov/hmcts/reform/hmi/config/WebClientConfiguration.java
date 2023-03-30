package uk.gov.hmcts.reform.hmi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures the Web Client that is used in requests to external services.
 */
@Configuration
@Profile({"!test", "!non-async"})
@EnableAsync
public class WebClientConfiguration {

    @Bean
    WebClient webClientInsecure() {
        return WebClient.builder().build();
    }
}
