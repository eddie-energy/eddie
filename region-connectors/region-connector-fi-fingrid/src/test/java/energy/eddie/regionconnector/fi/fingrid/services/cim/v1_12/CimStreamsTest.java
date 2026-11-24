// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_12;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CimStreamsTest {
    @Mock
    private EnergyDataService energyDataService;

    @Test
    void publishEmitsVHD() {
        // Given
        var publisher = TestPublisher.<IdentifiableValidatedHistoricalData>create();
        when(energyDataService.getIdentifiableValidatedHistoricalDataStream()).thenReturn(publisher.flux());
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var streams = new CimStreams(energyDataService);

        // When
        StepVerifier.create(streams.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> publisher.emit(new IdentifiableValidatedHistoricalData(permissionRequest(), List.of(resp))))
                    .then(publisher::complete)
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void publishEmitsVHDEquivalentToV104() throws Exception {
        // Given
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var data = new IdentifiableValidatedHistoricalData(permissionRequest(), List.of(resp));
        when(energyDataService.getIdentifiableValidatedHistoricalDataStream()).thenReturn(Flux.just(data));
        var v104Streams = new energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04.CimStreams(energyDataService);
        var v112Streams = new CimStreams(energyDataService);

        // When
        var v104 = v104Streams.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();
        var v112 = v112Streams.getValidatedHistoricalDataMarketDocumentsStream().collectList().block();

        // Then
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        assertEquals(List.of(), ValidatedHistoricalDataVersionComparator.differences(v104.getFirst(), v112.getFirst()));
        var envelope = v112.getFirst();
        assertEquals("FI", envelope.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        var vhd = envelope.getVHDMarketDocument();
        assertEquals(vhd.getCreatedDateTime(), vhd.getTimeSeries().getFirst().getDateAndOrTimeDateTime());
        var xml = new XmlMessageSerde().serialize(envelope);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    private static FingridPermissionRequest permissionRequest() {
        return new FingridPermissionRequestBuilder().setPermissionId("pid")
                                                    .setConnectionId("cid")
                                                    .setDataNeedId("dnid")
                                                    .setStatus(PermissionProcessStatus.ACCEPTED)
                                                    .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                                                    .setStart(LocalDate.now(ZoneOffset.UTC))
                                                    .setEnd(LocalDate.now(ZoneOffset.UTC))
                                                    .setCustomerIdentification("cid")
                                                    .setGranularity(Granularity.PT15M)
                                                    .setLastMeterReadings(null)
                                                    .build();
    }
}
