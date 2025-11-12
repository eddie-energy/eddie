/*
 * SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
 * SPDX-License-Identifier: Apache-2.0
 */

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_92;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntermediateRealTimeMeasurementMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();
    private final MijnAansluitingConfiguration config = new MijnAansluitingConfiguration("keyId",
                                                                                         "https://localhost",
                                                                                         new ClientID("client-id"),
                                                                                         Scope.parse("scope"),
                                                                                         URI.create("https://localhost"),
                                                                                         "token",
                                                                                         URI.create("https://localhost"));

    IntermediateRealTimeMeasurementMarketDocumentTest() throws SerdeInitializationException {}

    @Test
    void givenMijnAansluitingResponse_whenValue_thenReturnsNearRealTimeMeasurementMarketDocument() throws SerializationException {
        // Given
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <ns2:ReferenceEnergyCurveNearRealTimeMeasurement_MarketDocument xmlns:ns2="https://eddie.energy/CIM/NRTMD_v0.92">
                	<ns2:mRID>mrid</ns2:mRID>
                	<ns2:revisionNumber>92</ns2:revisionNumber>
                	<ns2:type>A45</ns2:type>
                	<ns2:createdDateTime>2025-07-16T08:03:09Z</ns2:createdDateTime>
                	<ns2:sender_MarketParticipant.mRID codingScheme="NNL">ESDN</ns2:sender_MarketParticipant.mRID>
                	<ns2:sender_MarketParticipant.marketRole.type>A26</ns2:sender_MarketParticipant.marketRole.type>
                	<ns2:receiver_MarketParticipant.mRID codingScheme="NNL">client-id</ns2:receiver_MarketParticipant.mRID>
                	<ns2:receiver_MarketParticipant.marketRole.type>A13</ns2:receiver_MarketParticipant.marketRole.type>
                	<ns2:process.processType>A16</ns2:process.processType>
                	<ns2:period.timeInterval>
                		<ns2:start>2025-01-01T00:00Z</ns2:start>
                		<ns2:end>2025-01-02T00:00Z</ns2:end>
                	</ns2:period.timeInterval>
                	<ns2:Series>
                	    <ns2:registeredResource.mRID codingScheme="NNL">register-mrid</ns2:registeredResource.mRID>
                	    <ns2:businessType>A04</ns2:businessType>
                	    <ns2:curveType>A02</ns2:curveType>
                	    <ns2:marketProduct.marketProductType>8716867000016</ns2:marketProduct.marketProductType>
                	    <ns2:flowDirection.direction>A02</ns2:flowDirection.direction>
                	    <ns2:resourceTimeSeries.value1ScheduleType>load</ns2:resourceTimeSeries.value1ScheduleType>
                	    <ns2:marketEvaluationPoint.mRID codingScheme="NNL">market-evaluation-point</ns2:marketEvaluationPoint.mRID>
                	    <ns2:Series>
                	        <ns2:measurement_Unit.name>KWH</ns2:measurement_Unit.name>
                	        <ns2:Period>
                	            <ns2:resolution>P0Y0M1DT0H0M0.000S</ns2:resolution>
                	            <ns2:timeInterval>
                		            <ns2:start>2025-01-01T00:00Z</ns2:start>
                		            <ns2:end>2025-01-02T00:00Z</ns2:end>
                	            </ns2:timeInterval>
                	            <ns2:Point>
                	                <ns2:position>1</ns2:position>
                	                <ns2:measured_Quantity.quantity>1</ns2:measured_Quantity.quantity>
                	                <ns2:measured_Quantity.quality>A04</ns2:measured_Quantity.quality>
                	            </ns2:Point>
                	        </ns2:Period>
                	    </ns2:Series>
                	</ns2:Series>
                </ns2:ReferenceEnergyCurveNearRealTimeMeasurement_MarketDocument>
                """;
        var id = new IdentifiableMeteredData(createPermissionRequest(), List.of(createMijnAansluitingResponse()));
        var intermediate = new IntermediateRealTimeMeasurementMarketDocument(id, config);

        // When
        var res = intermediate.value();

        // Then
        var ignoredNames = ignoredNames();
        var bytes = serde.serialize(res.getFirst());
        var testXml = new String(bytes, StandardCharsets.UTF_8);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(testXml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV092ReferenceEnergyCurveNearRealTimeMeasurementMarketDocument(bytes));
    }

    private static @NotNull MijnAansluitingPermissionRequest createPermissionRequest() {
        return new MijnAansluitingPermissionRequest("pid",
                                                    "cid",
                                                    "dnid",
                                                    PermissionProcessStatus.ACCEPTED,
                                                    "",
                                                    "",
                                                    ZonedDateTime.now(
                                                            ZoneOffset.UTC),
                                                    LocalDate.of(2025, 1, 1),
                                                    LocalDate.of(2025, 1, 1),
                                                    Granularity.P1D,
                                                    null,
                                                    null);
    }

    private static MijnAansluitingResponse createMijnAansluitingResponse() {
        var dateTime = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new MijnAansluitingResponse(
                new MarketEvaluationPoint(
                        "market-evaluation-point",
                        List.of(new Register(
                                new Meter("1.8.1"),
                                "register-mrid",
                                List.of(new Reading(
                                        new DateAndOrTime(dateTime),
                                        new ReadingType(ReadingType.MultiplierEnum.K,
                                                        ReadingType.UnitEnum.WH),
                                        BigDecimal.ONE
                                ))
                        ))
                )
        );
    }

    private static Set<String> ignoredNames() {
        return Set.of("createdDateTime", "mRID");
    }
}