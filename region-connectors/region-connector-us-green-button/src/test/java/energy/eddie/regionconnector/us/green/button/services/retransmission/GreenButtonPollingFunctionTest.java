package energy.eddie.regionconnector.us.green.button.services.retransmission;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PollingService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GreenButtonPollingFunctionTest {
    @Mock
    private PollingService pollingService;
    @InjectMocks
    private GreenButtonPollingFunction pollingFunc;

    @Test
    void testPoll_forEmptyResponse_returnsDataNotAvailable() {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("dk-energinet", "pid", from, to);
        when(pollingService.forcePollValidatedHistoricalData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Flux.empty());
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
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.forcePollValidatedHistoricalData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Flux.error(new RuntimeException("message")));
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
    void testPoll_forValidResponse_returnsSuccess() throws FeedException {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        var xml = XmlLoader.xmlFromResource("/xml/batch/Batch.xml");
        var payload = new IdentifiableSyndFeed(pr, new SyndFeedInput().build(new StringReader(xml)));

        when(pollingService.forcePollValidatedHistoricalData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Flux.just(payload));
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
    void testPoll_forPartiallyEmptyResponse_returnsDataEmpty() throws FeedException {
        // Given
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid").build();
        var xml = XmlLoader.xmlFromResource("/xml/batch/BatchWithoutIntervalBlock.xml");
        var payload = new IdentifiableSyndFeed(pr, new SyndFeedInput().build(new StringReader(xml)));
        var from = LocalDate.of(2025, 1, 1);
        var to = LocalDate.of(2025, 2, 1);
        var request = new RetransmissionRequest("cds", "pid", from, to);
        when(pollingService.forcePollValidatedHistoricalData(pr, from.atStartOfDay(ZoneOffset.UTC), endOfDay(to, ZoneOffset.UTC)))
                .thenReturn(Flux.just(payload));
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