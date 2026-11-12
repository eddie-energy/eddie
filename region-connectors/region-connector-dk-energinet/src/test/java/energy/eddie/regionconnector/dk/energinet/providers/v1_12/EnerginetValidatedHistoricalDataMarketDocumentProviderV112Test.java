// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.dk.energinet.DtoLoader;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v1_04.EnerginetValidatedHistoricalDataMarketDocumentProvider;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class EnerginetValidatedHistoricalDataMarketDocumentProviderV112Test {
    private final PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
            "ep-id"
    );
    private final XmlMessageSerde serde = new XmlMessageSerde();

    EnerginetValidatedHistoricalDataMarketDocumentProviderV112Test() throws SerdeInitializationException {}

    @Test
    void givenValidatedHistoricalData_whenReceived_thenPublishValidatedHistoricalDataMarketDocument() throws IOException {
        // Given
        var streams = new EnergyDataStreams();
        var provider = new EnerginetValidatedHistoricalDataMarketDocumentProviderV112(streams, cimConfig);
        var response = response();

        // When
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> {
                        streams.publish(response);
                        streams.close();
                    })
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void givenValidatedHistoricalData_whenReceived_thenDocumentIsEquivalentToV104AndValid() throws IOException, ExecutionException, InterruptedException, SerializationException {
        // Given
        var streams = new EnergyDataStreams();
        var v104Future = new EnerginetValidatedHistoricalDataMarketDocumentProvider(streams, cimConfig)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();
        var v112Future = new EnerginetValidatedHistoricalDataMarketDocumentProviderV112(streams, cimConfig)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();

        // When
        streams.publish(response());
        streams.close();

        // Then
        var v104 = v104Future.get();
        var v112 = v112Future.get();
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        assertEquals(List.of(), ValidatedHistoricalDataVersionComparator.differences(v104.getFirst(), v112.getFirst()));
        var envelope = v112.getFirst();
        assertEquals("DK", envelope.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        var vhd = envelope.getVHDMarketDocument();
        vhd.getTimeSeries().forEach(ts -> assertEquals(vhd.getCreatedDateTime(), ts.getDateAndOrTimeDateTime()));
        var xml = serde.serialize(envelope);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    private static IdentifiableApiResponse response() throws IOException {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var pr = new EnerginetPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                "mid",
                                                "token",
                                                now.toLocalDate(),
                                                now.toLocalDate(),
                                                Granularity.P1D,
                                                null,
                                                PermissionProcessStatus.ACCEPTED,
                                                now,
                                                null);
        var response = DtoLoader.loadValidatedHistoricalData().getResult();
        return new IdentifiableApiResponse(pr, Objects.requireNonNull(response).getFirst());
    }
}
