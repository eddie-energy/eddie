// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fi.fingrid.TestResourceProvider;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import energy.eddie.regionconnector.fi.fingrid.services.cim.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

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
        var pr = new FingridPermissionRequestBuilder().setPermissionId("pid")
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
        var resp = TestResourceProvider.readTimeSeriesFromFile(TestResourceProvider.TIME_SERIES_WITH_VALUES);
        var streams = new CimStreams(energyDataService);

        // When
        StepVerifier.create(streams.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> publisher.emit(new IdentifiableValidatedHistoricalData(pr, List.of(resp))))
                    .then(publisher::complete)
                    // Then
                    .expectNextCount(1)
                    .verifyComplete();
    }
}