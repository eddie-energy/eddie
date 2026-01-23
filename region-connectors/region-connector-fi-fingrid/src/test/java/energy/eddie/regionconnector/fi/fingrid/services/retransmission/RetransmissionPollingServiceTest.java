// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.fi.fingrid.client.model.*;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequestBuilder;
import energy.eddie.regionconnector.fi.fingrid.services.PollingService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionPollingServiceTest {
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private RetransmissionPollingService pollingFunc;

    @Test
    void testPoll_forEmptyResponse_returnsDataNotAvailable() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("dk-energinet", "pid", from, to);
        when(pollingService.forcePoll(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.empty());
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .isInstanceOf(DataNotAvailable.class)
                            .extracting(RetransmissionResult::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    @Test
    void testPoll_forError_returnsFailure() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.forcePoll(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.error(new RuntimeException("message")));
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .asInstanceOf(InstanceOfAssertFactories.type(Failure.class))
                            .satisfies(failure -> {
                                assertThat(failure.permissionId()).isEqualTo("pid");
                                assertThat(failure.reason()).isEqualTo("message");
                            })
                    )
                    .verifyComplete();
    }

    @Test
    void testPoll_forValidResponse_returnsSuccess() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        var party = new Party("id");
        var payload = List.of(
                new TimeSeriesResponse(
                        new TimeSeriesData(
                                new Header("id",
                                           party,
                                           party,
                                           party,
                                           party,
                                           "",
                                           ZonedDateTime.now(ZoneOffset.UTC),
                                           "d",
                                           "tpf"),
                                new TimeSeriesTransaction(
                                        null,
                                        null,
                                        List.of()
                                )
                        )
                )
        );
        when(pollingService.forcePoll(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.just(payload));
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                            .extracting(Success::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    @Test
    void testPoll_forPartiallyEmptyResponse_returnsDataEmpty() {
        // Given
        var pr = new FingridPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);

        List<TimeSeriesResponse> payload = List.of();
        when(pollingService.forcePoll(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.just(payload));
        // When
        var res = pollingFunc.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .isInstanceOf(DataNotAvailable.class)
                            .extracting(RetransmissionResult::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }
}