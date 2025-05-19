package energy.eddie.regionconnector.nl.mijn.aansluiting.retransmission;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MijnAansluitingPollingFunctionTest {
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private MijnAansluitingPollingFunction pollingFunc;

    @Test
    void testPoll_forEmptyResponse_returnsDataNotAvailable() {
        // Given
        var pr = createPermissionRequest();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("nl-mijn-aansluiting", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from, to)).thenReturn(Mono.empty());
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
        var pr = createPermissionRequest();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("nl-mijn-aansluiting", "pid", from, to);
        when(pollingService.pollTimeSeriesData(pr, from, to))
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
        var pr = createPermissionRequest();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("nl-mijn-aansluiting", "pid", from, to);
        var payload = Map.of("meterId", ZonedDateTime.now(ZoneOffset.UTC));
        when(pollingService.pollTimeSeriesData(pr, from, to)).thenReturn(Mono.just(payload));
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

    private static MijnAansluitingPermissionRequest createPermissionRequest() {
        return new MijnAansluitingPermissionRequest("pid",
                                                    "cid",
                                                    "dnid",
                                                    PermissionProcessStatus.ACCEPTED,
                                                    "state",
                                                    "verifier",
                                                    ZonedDateTime.now(ZoneOffset.UTC),
                                                    LocalDate.now(ZoneOffset.UTC),
                                                    LocalDate.now(ZoneOffset.UTC),
                                                    Granularity.PT15M);
    }
}