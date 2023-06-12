package uk.gov.hmcts.reform.hmi.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
public class DistributionService {

    private final WebClient webClient;
    private final String url;
    private final String destinationSystem;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",
                                                                           new Locale("eng", "GB"));

    private final RateLimiter rateLimiter;

    public DistributionService(@Autowired WebClient webClient,  @Value("${service-to-service.hmi-apim}") String url,
                               @Value("${destination-system}") String destinationSystem,
                               @Value("${request-limit}") int requestLimit) {
        this.webClient = webClient;
        this.url = url;
        this.destinationSystem = destinationSystem;
        this.rateLimiter = RateLimiter.of("hmi-rate-limiter", RateLimiterConfig
            .custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(requestLimit)
            .timeoutDuration(Duration.ofMinutes(1))
            .build());
    }

    @Async
    public Future<String> sendProcessedJson(String jsonData) {
        try {
            Future<String> apiResponse = webClient.post().uri(url + "/schedules")
                .attributes(clientRegistrationId("hmiApim"))
                .header("Source-System", "CRIME")
                .header("Destination-System", destinationSystem)
                .header("Request-Created-At", simpleDateFormat.format(new Date()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(BodyInserters.fromValue(jsonData))
                .retrieve()
                .onStatus(
                    HttpStatus.BAD_REQUEST::equals,
                    response -> response.bodyToMono(String.class).map(Exception::new))
                .bodyToMono(String.class)
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .toFuture();
            log.info(String.format("Json data has been sent with response: %s", apiResponse.get()));
            return apiResponse;
        } catch (Exception ex) { //NOSONAR
            log.error("Error response from HMI APIM:", ex.getMessage());
            return CompletableFuture.completedFuture(ex.getMessage());
        }
    }
}
