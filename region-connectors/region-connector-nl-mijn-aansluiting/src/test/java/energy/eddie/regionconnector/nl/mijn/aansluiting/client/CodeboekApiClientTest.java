package energy.eddie.regionconnector.nl.mijn.aansluiting.client;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoints;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CodeboekApiClientTest {
    private static final MockWebServer SERVER = new MockWebServer();
    private CodeboekApiClient client;

    @BeforeAll
    static void startServer() throws IOException {
        SERVER.start();
    }

    @BeforeEach
    void setupClient() {
        client = new CodeboekApiClient(new MijnAansluitingConfiguration(
                "continuous-id",
                "http://localhost",
                new ClientID("id"),
                new Scope("scope"),
                SERVER.url("/").uri(),
                "api-token",
                URI.create("http://localhost")
        ), WebClient.builder());
    }

    @AfterAll
    static void teardown() throws IOException {
        SERVER.shutdown();
    }

    @Test
    void meteringPoints_returnsMeteringPoints() throws IOException {
        // Given
        var body = JsonResourceObjectMapper.loadRawTestJson("codeboek_response.json");
        SERVER.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(body));
        SERVER.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(body));
        // When
        var res = client.meteringPoints("9999AB", "11");

        // Then
        StepVerifier.create(res)
                    .assertNext(meteringPoints -> assertThat(meteringPoints)
                            .extracting(MeteringPoints::getMeteringPoints,
                                        InstanceOfAssertFactories.list(MeteringPoint.class))
                            .singleElement()
                            .satisfies(meteringPoint -> {
                                assertThat(meteringPoint.getEan()).isEqualTo("871694840000000000");
                                assertThat(meteringPoint.getProduct()).isEqualTo(MeteringPoint.ProductEnum.ELK);
                                assertThat(meteringPoint.getOrganisation()).isEqualTo("ESDN");
                                assertThat(meteringPoint.getGridOperatorEan()).isEqualTo("8716948000000");
                                assertThat(meteringPoint.getGridArea()).isEqualTo("871694830000000000");
                                assertThat(meteringPoint.getBagId()).isEqualTo("0010010000000000");
                                assertThat(meteringPoint.getSpecialMeteringPoint()).isFalse();
                            })
                            .extracting(MeteringPoint::getAddress)
                            .satisfies(address -> {
                                assertThat(address.getPostalCode()).isEqualTo("9999AB");
                                assertThat(address.getStreetNumber()).isEqualTo(11);
                                assertThat(address.getStreet()).isEqualTo("example street");
                                assertThat(address.getCity()).isEqualTo("Amsterdam");
                            }))
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void meteringPoints_setsHealth() {
        // Given
        // language=JSON
        var body = """
                {
                  "meteringPoints": []
                }
                """;
        SERVER.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setHeader("Content-Type", "application/json")
                               .setBody(body));
        // When
        var res = client.meteringPoints("9999AB", "11", MeteringPoint.ProductEnum.ELK);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
        assertEquals(Status.UP, client.health().getStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503})
    void meteringPoints_withServerError_setsHealthToDown(int status) {
        // Given
        // language=JSON
        SERVER.enqueue(new MockResponse().setResponseCode(status));
        // When
        var res = client.meteringPoints("9999AB", "11", MeteringPoint.ProductEnum.ELK);

        // Then
        StepVerifier.create(res)
                    .expectError()
                    .verify();
        assertEquals(Status.DOWN, client.health().getStatus());
    }

    @Test
    void meteringPoints_withUnrelatedError_doesNotSetHealth() {
        // Given
        SERVER.enqueue(new MockResponse().setStatus("jfajfdlkasjfkl"));

        // When
        var res = client.meteringPoints("9999AB", "11", MeteringPoint.ProductEnum.ELK);

        // Then
        StepVerifier.create(res)
                    .expectError()
                    .verify();
        assertEquals(Status.UNKNOWN, client.health().getStatus());
    }
}