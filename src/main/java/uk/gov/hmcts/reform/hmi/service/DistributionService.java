package uk.gov.hmcts.reform.hmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Slf4j
@Service
public class DistributionService {

    @Value("${url}")
    private String url;

    @Autowired
    WebClient webClient;

    public boolean sendBlobName(String blobName) {
        try {
            webClient.post().uri(url + "/poc")
                         .body(BodyInserters.fromValue(blobName)).retrieve()
                         .bodyToMono(String.class).block();
            log.info("Latest blob name has been sent");
            return true;
        } catch (WebClientException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return false;
        }
    }
}
