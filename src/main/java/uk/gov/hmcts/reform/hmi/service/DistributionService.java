package uk.gov.hmcts.reform.hmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
public class DistributionService {

    private final String url;
    private final WebClient webClient;

    public DistributionService(@Autowired WebClient webClient,  @Value("${service-to-service.hmi-apim}") String url) {
        this.webClient = webClient;
        this.url = url;
    }

    public Future<Boolean> sendProcessedJson(String jsonData) {
        try {
            webClient.post().uri(url + "/schedules")
                .attributes(clientRegistrationId("hmiApim"))
                .header("Source-System", "CRIME")
                .header("Destination-System", "MOCK") //TODO Change this
                .header("Request-Created-At", LocalDateTime.now().toString())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(BodyInserters.fromValue(jsonData)).retrieve()
                .bodyToMono(String.class).block();
            log.info("Json data has been sent");
            return CompletableFuture.completedFuture(true);
        } catch (Exception ex) {
            log.error("Error response from HMI APIM:", ex);

            return CompletableFuture.completedFuture(false);
        }
    }
}
