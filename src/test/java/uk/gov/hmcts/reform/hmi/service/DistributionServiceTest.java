package uk.gov.hmcts.reform.hmi.service;

import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("PMD.LawOfDemeter")
@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class DistributionServiceTest {

    private static MockWebServer mockWebServerEndpoint;

    LogCaptor logCaptor = LogCaptor.forClass(DistributionService.class);

    DistributionService distributionService;

    @BeforeEach
    void setup() throws IOException {
        WebClient webClient = WebClient.create();
        distributionService = new DistributionService(webClient, "http://localhost:1234");
        mockWebServerEndpoint = new MockWebServer();
        mockWebServerEndpoint.start(1234);
    }

    @AfterEach
    void teardown() throws IOException {
        mockWebServerEndpoint.shutdown();
    }

    @Test
    void testSendBlobName() {
        mockWebServerEndpoint.enqueue(new MockResponse().setBody("Email has been sent"));

        distributionService.sendBlobName("Blob name");
        assertTrue(logCaptor.getInfoLogs().get(0).contains("Latest blob name has been sent"),
                   "Info log did not contain message");
    }

    @Test
    void testSendBlobNameFailed() {
        mockWebServerEndpoint.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value()));

        distributionService.sendBlobName("Blob name");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Request failed with error message:"),
                   "Error logs did not contain message");
    }
}
