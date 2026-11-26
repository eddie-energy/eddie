// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.outbound.rest.TestDataSourceInformation;
import energy.eddie.outbound.rest.dto.ConnectionStatusMessages;
import energy.eddie.outbound.rest.dto.OpaqueEnvelopes;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FallbackXmlMessageConverterTest {
    private final FallbackXmlMessageConverter converter = new FallbackXmlMessageConverter(new JacksonXmlHttpMessageConverter());

    @ParameterizedTest
    @MethodSource("cimAndAgnosticClasses")
    void canRead_forAgnosticAndCimClasses_returnsTrue(Class<?> clazz) {
        // Given
        // When
        var res = converter.canRead(clazz, MediaType.APPLICATION_XML);

        // Then
        assertTrue(res);
    }

    @ParameterizedTest
    @MethodSource("cimAndAgnosticClasses")
    void canWrite_forAgnosticAndCimClasses_returnsTrue(Class<?> clazz) {
        // Given
        // When
        var res = converter.canWrite(clazz, MediaType.APPLICATION_XML);

        // Then
        assertTrue(res);
    }

    @Test
    // False positive by sonar
    @SuppressWarnings("java:S1612")
    void getSupportedMediaTypes_doesNotThrow() {
        // Given & When & Then
        assertDoesNotThrow(() -> converter.getSupportedMediaTypes());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void read_validatedHistoricalDataMarketDocuments_returnsHistoricalDataMarketDocuments() throws IOException {
        // Given
        var xml = getClass().getResourceAsStream("/vhds.xml");
        var msg = new MockHttpInputMessage(xml);

        // When
        var res = converter.read(ValidatedHistoricalDataMarketDocuments.class, msg);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(ValidatedHistoricalDataMarketDocuments.class))
                .extracting(ValidatedHistoricalDataMarketDocuments::getDocuments)
                .asInstanceOf(InstanceOfAssertFactories.list(ValidatedHistoricalDataEnvelope.class))
                .singleElement()
                .extracting(ValidatedHistoricalDataEnvelope::getValidatedHistoricalDataMarketDocument)
                .extracting(ValidatedHistoricalDataMarketDocumentComplexType::getMRID)
                .isEqualTo("3847de2f-45f8-44e8-8a68-591635477f94");
    }

    @Test
    void read_permissionCommand_returnsTypedCommand() throws IOException {
        // Given
        // language=XML
        var xml = """
                <PermissionCommand>
                    <action>TERMINATE</action>
                    <regionConnectorId>aiida</regionConnectorId>
                    <permissionId>11162130-a49f-4705-aa1a-3165c725f69f</permissionId>
                </PermissionCommand>
                """;
        var msg = new MockHttpInputMessage(xml.getBytes(StandardCharsets.UTF_8));

        // When
        var res = converter.read(PermissionCommand.class, msg);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(PermissionCommand.Terminate.class))
                .satisfies(terminate -> {
                    assertEquals("aiida", terminate.regionConnectorId());
                    assertEquals(UUID.fromString("11162130-a49f-4705-aa1a-3165c725f69f"), terminate.permissionId());
                });
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void write_validatedHistoricalDataMarketDocuments_returnsHistoricalDataMarketDocuments() throws IOException {
        // Given
        var xml = getClass().getResourceAsStream("/vhds.xml");
        var vhds = (ValidatedHistoricalDataMarketDocuments) converter.read(ValidatedHistoricalDataMarketDocuments.class,
                                                                           new MockHttpInputMessage(xml));
        var vhd = vhds.getDocuments().getFirst();
        var msg = new MockHttpOutputMessage();

        // When
        converter.write(vhd, MediaType.APPLICATION_XML, msg);

        // Then
        var res = msg.getBodyAsString();
        assertTrue(XmlValidator.validateValidatedHistoricalMarketDocument(res));
    }

    @Test
    void write_validatedHistoricalDataMarketDocumentsV1_12_returnsXml() throws IOException {
        // Given
        var envelope = new energy.eddie.cim.v1_12.vhd.Envelope()
                .withMessageDocumentHeader(
                        new energy.eddie.cim.v1_12.vhd.MessageDocumentHeader()
                                .withMetaInformation(
                                        new energy.eddie.cim.v1_12.vhd.MetaInformation()
                                                .withRequestPermissionId("pid")
                                                .withRegionCountry("AT")
                                )
                )
                .withVHDMarketDocument(new energy.eddie.cim.v1_12.vhd.VHDMarketDocument().withMRID("mrid"));
        var docs = new energy.eddie.outbound.rest.dto.v1_12.ValidatedHistoricalDataMarketDocuments(List.of(envelope));
        var collectionMsg = new MockHttpOutputMessage();
        var singleMsg = new MockHttpOutputMessage();

        // When
        converter.write(docs, MediaType.APPLICATION_XML, collectionMsg);
        converter.write(envelope, MediaType.APPLICATION_XML, singleMsg);

        // Then
        var collection = collectionMsg.getBodyAsString();
        assertAll(
                () -> assertTrue(collection.contains("ValidatedHistoricalDataMarketDocuments"), collection),
                () -> assertTrue(collection.contains("https://insieme.energy/CEEDS_ValidatedHistoricalDataDocument_v1.12.38"),
                                 collection),
                () -> assertTrue(collection.contains("<ns2:mRID>mrid</ns2:mRID>") || collection.contains(">mrid<"),
                                 collection)
        );
        // Schema compliance of complete documents is covered by the CIM and region connector tests
        var single = singleMsg.getBodyAsString();
        assertTrue(single.contains("Envelope") && single.contains(">pid<"), single);
    }

    @Test
    void read_validatedHistoricalDataMarketDocumentsV1_12_returnsDocuments() throws IOException {
        // Given
        var envelope = new energy.eddie.cim.v1_12.vhd.Envelope()
                .withVHDMarketDocument(new energy.eddie.cim.v1_12.vhd.VHDMarketDocument().withMRID("mrid"));
        var out = new MockHttpOutputMessage();
        converter.write(new energy.eddie.outbound.rest.dto.v1_12.ValidatedHistoricalDataMarketDocuments(List.of(envelope)),
                        MediaType.APPLICATION_XML,
                        out);

        // When
        var res = converter.read(energy.eddie.outbound.rest.dto.v1_12.ValidatedHistoricalDataMarketDocuments.class,
                                 new MockHttpInputMessage(out.getBodyAsBytes()));

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(energy.eddie.outbound.rest.dto.v1_12.ValidatedHistoricalDataMarketDocuments.class))
                .extracting(energy.eddie.outbound.rest.dto.v1_12.ValidatedHistoricalDataMarketDocuments::getDocuments)
                .asInstanceOf(InstanceOfAssertFactories.list(energy.eddie.cim.v1_12.vhd.Envelope.class))
                .singleElement()
                .extracting(e -> e.getVHDMarketDocument().getMRID())
                .isEqualTo("mrid");
    }

    @Test
    void write_connectionStatusMessage_doesNotThrow() {
        // Given
        var csm = new ConnectionStatusMessages(List.of(
                new ConnectionStatusMessage(
                        "cid",
                        "pid",
                        "dnid",
                        new TestDataSourceInformation("AT", "at-eda", "eda", "eda"),
                        PermissionProcessStatus.CREATED
                )
        ));
        var msg = new MockHttpOutputMessage();

        // When & Then
        assertDoesNotThrow(() -> converter.write(csm, MediaType.APPLICATION_XML, msg));
    }

    @Test
    void write_opaqueEnvelope_doesNotThrow() {
        // Given
        var opaque = new OpaqueEnvelopes(List.of(
                new OpaqueEnvelope("rid",
                                   "pid",
                                   "cid",
                                   "dnid",
                                   "mid",
                                   ZonedDateTime.parse("2026-06-19T14:06:00Z"),
                                   "{}")
        ));
        var msg = new MockHttpOutputMessage();

        // When & Then
        assertDoesNotThrow(() -> converter.write(opaque, MediaType.APPLICATION_XML, msg));
    }

    private static Stream<Arguments> cimAndAgnosticClasses() {
        return Stream.of(
                Arguments.of(ValidatedHistoricalDataMarketDocuments.class),
                Arguments.of(energy.eddie.outbound.rest.dto.v1_12.ValidatedHistoricalDataMarketDocuments.class),
                Arguments.of(energy.eddie.cim.v1_12.vhd.Envelope.class),
                Arguments.of(ConnectionStatusMessages.class)
        );
    }
}