// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.exceptions.EtaPlusOperationExceptions.RateLimitException;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock private EtaPlusApiClient apiClient;
    @Mock private ValidatedHistoricalDataStream stream;
    @Mock private Outbox outbox;

    @InjectMocks private PollingService pollingService;

    private static DePermissionRequest request(LocalDate start, LocalDate end, String accessToken, LocalDate watermark) {
        DePermissionRequestBuilder builder = new DePermissionRequestBuilder()
                .permissionId("pid")
                .connectionId("cid")
                .meteringPointId("malo-1")
                .dataNeedId("dnid")
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID))
                .start(start)
                .end(end)
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.ELECTRICITY);
        if (accessToken != null) {
            builder.accessToken(accessToken);
        }
        if (watermark != null) {
            builder.latestMeterReadingEndDate(watermark);
        }
        return builder.build();
    }

    @Test
    void pollTimeSeriesData_fetchesAndPublishes_whenAccessTokenPresent() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(30), today.plusDays(30), "token-abc", null);
        EtaPlusMeteredData data = new EtaPlusMeteredData("malo-1", pr.start(), today.minusDays(1), List.of());

        when(apiClient.fetchMeteredData(pr, "token-abc")).thenReturn(Mono.just(data));

        pollingService.pollTimeSeriesData(pr);

        verify(apiClient).fetchMeteredData(pr, "token-abc");
        verify(stream).publish(pr, data);
        verifyNoInteractions(outbox);
    }

    @Test
    void pollTimeSeriesData_skips_whenAccessTokenMissing() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(30), today.plusDays(30), null, null);

        pollingService.pollTimeSeriesData(pr);

        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
        verifyNoInteractions(outbox);
    }

    @Test
    void pollTimeSeriesData_commitsRevoked_onForbidden() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(30), today.plusDays(30), "token-abc", null);
        WebClientResponseException forbidden = WebClientResponseException.create(
                HttpStatus.FORBIDDEN.value(), "Forbidden", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(apiClient.fetchMeteredData(pr, "token-abc")).thenReturn(Mono.error(forbidden));

        pollingService.pollTimeSeriesData(pr);

        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(captor.getValue().status()).isEqualTo(PermissionProcessStatus.REVOKED);
        assertThat(captor.getValue().permissionId()).isEqualTo("pid");
        verifyNoInteractions(stream);
    }

    @Test
    void pollTimeSeriesData_commitsUnableToSend_onRateLimit() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(30), today.plusDays(30), "token-abc", null);
        when(apiClient.fetchMeteredData(pr, "token-abc"))
                .thenReturn(Mono.error(new RateLimitException("rate limit")));

        pollingService.pollTimeSeriesData(pr);

        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(captor.getValue().status()).isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
        verifyNoInteractions(stream);
    }

    @Test
    void pollTimeSeriesData_commitsUnableToSend_onGenericError() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(30), today.plusDays(30), "token-abc", null);
        when(apiClient.fetchMeteredData(pr, "token-abc"))
                .thenReturn(Mono.error(new RuntimeException("unexpected")));

        pollingService.pollTimeSeriesData(pr);

        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox).commit(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SimpleEvent.class);
        assertThat(captor.getValue().status()).isEqualTo(PermissionProcessStatus.UNABLE_TO_SEND);
        verifyNoInteractions(stream);
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsTrue_whenStartedAndNoWatermark() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(10), today.plusDays(30), "token-abc", null);

        assertThat(pollingService.isActiveAndNeedsToBeFetched(pr)).isTrue();
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsTrue_whenWatermarkBeforeToday() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(10), today.plusDays(30), "token-abc", today.minusDays(1));

        assertThat(pollingService.isActiveAndNeedsToBeFetched(pr)).isTrue();
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_whenWatermarkIsToday() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.minusDays(10), today.plusDays(30), "token-abc", today);

        assertThat(pollingService.isActiveAndNeedsToBeFetched(pr)).isFalse();
    }

    @Test
    void isActiveAndNeedsToBeFetched_returnsFalse_whenStartIsInFuture() {
        LocalDate today = LocalDate.now(EtaRegionConnectorMetadata.DE_ZONE_ID);
        DePermissionRequest pr = request(today.plusDays(1), today.plusDays(30), "token-abc", null);

        assertThat(pollingService.isActiveAndNeedsToBeFetched(pr)).isFalse();
    }
}