// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.provider.v1_04.FluviusValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FluviusValidatedHistoricalDataMarketDocumentProviderV112Test {
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper();
    private final XmlMessageSerde serde = new XmlMessageSerde();
    private final FluviusOAuthConfiguration fluviusConfig = new FluviusOAuthConfiguration("http://localhost:8080",
                                                                                          "client-id",
                                                                                          "client-secret",
                                                                                          "tenant-id",
                                                                                          "scope");

    FluviusValidatedHistoricalDataMarketDocumentProviderV112Test() throws SerdeInitializationException {}

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_returnsDocument() {
        // Given
        var streams = new IdentifiableDataStreams();
        streams.publish(permissionRequest(Granularity.P1D), payload("electricity_data_measurement_quarter_hourly.json"));
        streams.close();
        var provider = new FluviusValidatedHistoricalDataMarketDocumentProviderV112(fluviusConfig, streams);

        // When & Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void getValidatedHistoricalDataMarketDocumentsStream_isEquivalentToV104AndValid(
            String inputJsonFile,
            Granularity granularity
    ) throws ExecutionException, InterruptedException, SerializationException {
        // Given
        var streams = new IdentifiableDataStreams();
        var v104Future = new FluviusValidatedHistoricalDataMarketDocumentProvider(fluviusConfig, streams)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();
        var v112Future = new FluviusValidatedHistoricalDataMarketDocumentProviderV112(fluviusConfig, streams)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();

        // When
        streams.publish(permissionRequest(granularity), payload(inputJsonFile));
        streams.close();

        // Then
        var v104 = v104Future.get();
        var v112 = v112Future.get();
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        assertEquals(List.of(), ValidatedHistoricalDataVersionComparator.differences(v104.getFirst(), v112.getFirst()));
        var xml = serde.serialize(v112.getFirst());
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
        var meta = v112.getFirst().getMessageDocumentHeader().getMetaInformation();
        assertEquals("BE", meta.getRegionCountry());
        var vhd = v112.getFirst().getVHDMarketDocument();
        vhd.getTimeSeries()
           .forEach(ts -> assertEquals(vhd.getCreatedDateTime(), ts.getDateAndOrTimeDateTime()));
    }

    private GetEnergyResponseModelApiDataResponse payload(String file) {
        return mapper.readValue(classLoader.getResourceAsStream(file), GetEnergyResponseModelApiDataResponse.class);
    }

    private static energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest permissionRequest(
            Granularity granularity
    ) {
        return DefaultFluviusPermissionRequestBuilder.create()
                                                     .permissionId("pid")
                                                     .connectionId("cid")
                                                     .dataNeedId("dnid")
                                                     .granularity(granularity)
                                                     .build();
    }

    private static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of("electricity_data_measurement_quarter_hourly.json", Granularity.PT15M),
                Arguments.of("electricity_data_headpoints_measurement_quarter_hourly.json", Granularity.PT15M),
                Arguments.of("electricity_data_headpoints_and_physical_meters_measurement_quarter_hourly.json",
                             Granularity.PT15M),
                Arguments.of("gas_data_measurement_hourly.json", Granularity.PT1H)
        );
    }
}
