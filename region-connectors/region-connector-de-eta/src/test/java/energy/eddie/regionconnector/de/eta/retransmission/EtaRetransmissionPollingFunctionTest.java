// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.credentials.DePermissionCredentials;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.persistence.DePermissionCredentialsRepository;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtaRetransmissionPollingFunctionTest {

    private static final String PERMISSION_ID = "pid";
    private static final String TOKEN = "token-abc";
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 2, 1);

    @Mock private EtaPlusApiClient apiClient;
    @Mock private ValidatedHistoricalDataStream stream;
    @Mock private DePermissionCredentialsRepository credentialsRepository;

    @InjectMocks private EtaRetransmissionPollingFunction pollingFunction;

    private DePermissionRequest request() {
        return new DePermissionRequestBuilder()
                .permissionId(PERMISSION_ID)
                .meteringPointId("malo-1")
                .start(FROM.minusDays(10))
                .end(TO.plusDays(10))
                .status(PermissionProcessStatus.ACCEPTED)
                .build();
    }

    private RetransmissionRequest retransmissionRequest() {
        return new RetransmissionRequest("de-eta", PERMISSION_ID, FROM, TO);
    }

    private void withCredentials() {
        when(credentialsRepository.findByPermissionId(PERMISSION_ID))
                .thenReturn(Optional.of(new DePermissionCredentials(PERMISSION_ID, TOKEN, null)));
    }

    @Test
    void poll_publishesAndReturnsSuccess_whenReadingsPresent() {
        DePermissionRequest pr = request();
        EtaPlusMeteredData data = new EtaPlusMeteredData("malo-1", FROM, TO, List.of(
                new EtaPlusMeteredData.MeterReading(
                        ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1.0, "kWh", "VALIDATED", "Consumption")));
        withCredentials();
        when(apiClient.fetchMeteredData(pr, TOKEN, FROM, TO)).thenReturn(Mono.just(data));

        StepVerifier.create(pollingFunction.poll(pr, retransmissionRequest()))
                    .assertNext(result -> assertThat(result)
                            .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                            .extracting(Success::permissionId)
                            .isEqualTo(PERMISSION_ID))
                    .verifyComplete();

        verify(stream).publishRetransmission(pr, data);
    }

    @Test
    void poll_returnsDataNotAvailable_andDoesNotPublish_whenNoReadings() {
        DePermissionRequest pr = request();
        EtaPlusMeteredData data = new EtaPlusMeteredData("malo-1", FROM, TO, List.of());
        withCredentials();
        when(apiClient.fetchMeteredData(pr, TOKEN, FROM, TO)).thenReturn(Mono.just(data));

        StepVerifier.create(pollingFunction.poll(pr, retransmissionRequest()))
                    .assertNext(result -> assertThat(result)
                            .isInstanceOf(DataNotAvailable.class)
                            .extracting(RetransmissionResult::permissionId)
                            .isEqualTo(PERMISSION_ID))
                    .verifyComplete();

        verifyNoInteractions(stream);
    }

    @Test
    void poll_returnsFailure_onApiError() {
        DePermissionRequest pr = request();
        withCredentials();
        when(apiClient.fetchMeteredData(pr, TOKEN, FROM, TO))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(pollingFunction.poll(pr, retransmissionRequest()))
                    .assertNext(result -> assertThat(result)
                            .asInstanceOf(InstanceOfAssertFactories.type(Failure.class))
                            .satisfies(failure -> {
                                assertThat(failure.permissionId()).isEqualTo(PERMISSION_ID);
                                assertThat(failure.reason()).isEqualTo("boom");
                            }))
                    .verifyComplete();

        verifyNoInteractions(stream);
    }

    @Test
    void poll_returnsFailure_andDoesNotCallApi_whenNoCredentials() {
        DePermissionRequest pr = request();
        when(credentialsRepository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.empty());

        StepVerifier.create(pollingFunction.poll(pr, retransmissionRequest()))
                    .assertNext(result -> assertThat(result)
                            .asInstanceOf(InstanceOfAssertFactories.type(Failure.class))
                            .extracting(Failure::permissionId)
                            .isEqualTo(PERMISSION_ID))
                    .verifyComplete();

        verifyNoInteractions(apiClient);
        verifyNoInteractions(stream);
    }
}