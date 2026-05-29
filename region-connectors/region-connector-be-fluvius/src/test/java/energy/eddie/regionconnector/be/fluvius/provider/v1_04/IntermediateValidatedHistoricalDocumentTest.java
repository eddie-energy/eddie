// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.builder.DiffBuilder;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDocumentTest {
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper();
    private final XmlMessageSerde serde = new XmlMessageSerde();

    private final FluviusOAuthConfiguration fluviusConfig = new FluviusOAuthConfiguration("http://localhost:8080",
                                                                                          "client-id",
                                                                                          "client-secret",
                                                                                          "tenant-id",
                                                                                          "scope");

    IntermediateValidatedHistoricalDocumentTest() throws SerdeInitializationException {}

    @ParameterizedTest
    @MethodSource("testToVhd")
    void testToVhd(
            String inputJsonFile,
            String expectedXmlFile,
            Granularity granularity
    ) throws SerializationException {
        // Given
        var json = classLoader.getResourceAsStream(inputJsonFile);
        var payload = mapper.readValue(json, GetEnergyResponseModelApiDataResponse.class);
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .connectionId("cid")
                                                       .dataNeedId("dnid")
                                                       .granularity(granularity)
                                                       .build();
        var doc = new IntermediateValidatedHistoricalDocument(fluviusConfig, new IdentifiableMeteringData(pr, payload));
        var ignoredNames = Set.of(
                "mRID",
                "messageDocumentHeader.creationDateTime"
        );
        var expectedStream = classLoader.getResourceAsStream(expectedXmlFile);
        assert expectedStream != null;
        var expected = new BufferedReader(new InputStreamReader(expectedStream))
                .lines()
                .collect(Collectors.joining("\n"));

        // When
        var res = doc.toVHD();

        // Then
        assertNotNull(res);
        var bytes = serde.serialize(res);
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }

    private static Stream<Arguments> testToVhd() {
        return Stream.of(
                Arguments.of(
                        "electricity_data_measurement_quarter_hourly.json",
                        "cim/v1_04/vhd_for_physical_meters.xml",
                        Granularity.PT15M
                ),
                Arguments.of(
                        "electricity_data_headpoints_measurement_quarter_hourly.json",
                        "cim/v1_04/vhd_for_headpoints.xml",
                        Granularity.PT15M
                ),
                Arguments.of(
                        "electricity_data_headpoints_and_physical_meters_measurement_quarter_hourly.json",
                        "cim/v1_04/vhd_for_headpoints_and_physical_meters.xml",
                        Granularity.PT15M
                ),
                Arguments.of(
                        "gas_data_measurement_hourly.json",
                        "cim/v1_04/vhd_for_physical_meters_for_gas.xml",
                        Granularity.PT1H
                )
        );
    }
}
