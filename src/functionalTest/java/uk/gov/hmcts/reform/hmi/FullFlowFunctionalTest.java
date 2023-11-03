package uk.gov.hmcts.reform.hmi;

import com.ginsberg.junit.exit.ExpectSystemExit;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.hmi.service.DistributionService;
import uk.gov.hmcts.reform.hmi.service.ProcessingService;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"PMD.UseUnderscoresInNumericLiterals", "PMD.CloseResource"})
@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = "functional")
class FullFlowFunctionalTest {

    private static final DockerComposeContainer DOCKER_COMPOSE_CONTAINER = new DockerComposeContainer(
        new File("src/functionalTest/resources/docker-compose-test.yml"))
        .withExposedService("azurite", 10000);

    private static final String ASSERTION_MESSAGE = "Expected and actual logs did not match";

    private MockWebServer hmiApimMockServer;

    @BeforeEach
    void setup() throws IOException {
        // setup mock web server
        hmiApimMockServer = new MockWebServer();
        hmiApimMockServer.start(1234);

        // Start docker compose which sets up blob storage with containers and test file
        DOCKER_COMPOSE_CONTAINER.start();
        // This is the Azurite Default Connection String. Keys are not secure.
        final var defaultAzuriteConnectionString =
            "DefaultEndpointsProtocol=http;"
                + "AccountName=devstoreaccount1;"
                + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
                + "BlobEndpoint=http://127.0.0.1:%s/devstoreaccount1;";

        String connectionString = String.format(
            defaultAzuriteConnectionString,
            DOCKER_COMPOSE_CONTAINER.getServicePort("azurite", 10000)
        );
        System.setProperty("CONNECTION_STRING", connectionString);
    }

    @AfterEach
    void teardown() throws IOException {
        hmiApimMockServer.close();
        DOCKER_COMPOSE_CONTAINER
            .withRemoveVolumes(true)
            .stop();
    }

    @Test
    @ExpectSystemExit
    void fullFlowToMockEndpoint() throws InterruptedException {
        // Setup mock web server to capture and respond to requests
        hmiApimMockServer.enqueue(new MockResponse()
                                      .setResponseCode(200)
                                      .setBody("The request was received successfully.")
        );

        hmiApimMockServer.enqueue(new MockResponse()
                                      .setResponseCode(200)
                                      .setBody("The request was received successfully.")
        );

        // Start the application
        Application.main(new String[]{});

        // Verify expected logs are thrown
        LogCaptor runnerLogCaptor = LogCaptor.forClass(Runner.class);
        assertTrue(runnerLogCaptor.getInfoLogs().contains("All blobs retrieved"), ASSERTION_MESSAGE);
        assertTrue(runnerLogCaptor.getInfoLogs().contains("Eligible blob selected to process"), ASSERTION_MESSAGE);

        LogCaptor processingLogCaptor = LogCaptor.forClass(ProcessingService.class);
        assertTrue(processingLogCaptor.getInfoLogs().contains("Download blob rota_test_file.xml"),
                   ASSERTION_MESSAGE);
        assertTrue(processingLogCaptor.getInfoLogs().contains("Blob rota_test_file.xml validation: true"),
                   ASSERTION_MESSAGE);

        LogCaptor distributionLogCaptor = LogCaptor.forClass(DistributionService.class);
        assertTrue(distributionLogCaptor.getInfoLogs().contains("The request was received successfully."),
                   ASSERTION_MESSAGE);
        assertTrue(runnerLogCaptor.getInfoLogs().contains("Blob processed, shutting down"),
                   ASSERTION_MESSAGE);

        // Verify payload being sent to the mocked api is as expected
        hmiApimMockServer.takeRequest();
        RecordedRequest recordedRequest = hmiApimMockServer.takeRequest();

        assertEquals("POST", recordedRequest.getMethod(), ASSERTION_MESSAGE);
        assertEquals("CRIME", recordedRequest.getHeader("Source-System"), ASSERTION_MESSAGE);
        assertEquals("SNL", recordedRequest.getHeader("Destination-System"), ASSERTION_MESSAGE);
    }
}
