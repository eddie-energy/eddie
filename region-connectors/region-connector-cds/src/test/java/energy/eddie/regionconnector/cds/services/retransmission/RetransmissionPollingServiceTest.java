package energy.eddie.regionconnector.cds.services.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.services.PollingService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionPollingServiceTest {
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private RetransmissionPollingService retransmissionPollingService;

    @Test
    void testPoll_forEmptyResponse_returnsDataNotAvailable() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.empty());
        // When
        var res = retransmissionPollingService.poll(pr, request);

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
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.error(new RuntimeException("message")));
        // When
        var res = retransmissionPollingService.poll(pr, request);

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
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        var result = Tuples.of(List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                               List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                               List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                               List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                               List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()));
        when(pollingService.pollTimeSeriesData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.just(result));
        // When
        var res = retransmissionPollingService.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                            .extracting(Success::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource
    void testPoll_forPartiallyEmptyResponse_returnsDataEmpty(
            Tuple5<List<AccountsEndpoint200ResponseAllOfAccountsInner>, List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>, List<ServicePointEndpoint200ResponseAllOfServicePointsInner>, List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>, List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>> tuple
    ) {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Mono.just(tuple));
        // When
        var res = retransmissionPollingService.poll(pr, request);

        // Then
        StepVerifier.create(res)
                    .assertNext(response -> assertThat(response)
                            .isInstanceOf(DataNotAvailable.class)
                            .extracting(RetransmissionResult::permissionId)
                            .isEqualTo("pid")
                    )
                    .verifyComplete();
    }

    @SuppressWarnings("unused") // errorprone  false positive
    private static Stream<Arguments> testPoll_forPartiallyEmptyResponse_returnsDataEmpty() {
        return Stream.of(
                Arguments.of(Tuples.of(List.of(),
                                       List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                                       List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                                       List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                                       List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()))),
                Arguments.of(Tuples.of(List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                                       List.of(),
                                       List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                                       List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                                       List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()))),
                Arguments.of(Tuples.of(List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                                       List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                                       List.of(),
                                       List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                                       List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()))),
                Arguments.of(Tuples.of(List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                                       List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                                       List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                                       List.of(),
                                       List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()))),
                Arguments.of(Tuples.of(List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                                       List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                                       List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                                       List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                                       List.of()))
        );
    }
}