package energy.eddie.core.services;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionServiceNotFound;
import energy.eddie.api.agnostic.retransmission.result.Success;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreRetransmissionRouterTest {
    public static final String PERMISSION_ID_1 = "p-id-1";
    public static final String PERMISSION_ID_2 = "p-id-2";
    public static final String PERMISSION_ID_3 = "p-id-3";
    public static final String REGION_CONNECTOR_ID_1 = "s1";
    public static final String REGION_CONNECTOR_ID_2 = "s2";
    public static final String REGION_CONNECTOR_ID_3 = "s3";
    @Mock
    private RegionConnectorRetransmissionService regionConnectorRetransmissionService;
    @Mock
    private RegionConnectorRetransmissionService regionConnectorRetransmissionService2;

    @Test
    // ZonedDateTime ignored in record needs to be there to compile
    @SuppressWarnings("unused")
    void registerRetransmissionConnector_routesRequestsToRegisteredServicesAndReturnsResults() throws Exception {
        // Given
        TestPublisher<RetransmissionRequest> testPublisher = TestPublisher.create();
        TestPublisher<RetransmissionRequest> testPublisher2 = TestPublisher.create();
        when(regionConnectorRetransmissionService.requestRetransmission(any())).thenReturn(Mono.just(new Success(
                PERMISSION_ID_1, ZonedDateTime.now(ZoneOffset.UTC))));
        when(regionConnectorRetransmissionService2.requestRetransmission(any())).thenReturn(Mono.just(new Success(
                PERMISSION_ID_2, ZonedDateTime.now(ZoneOffset.UTC))));

        CoreRetransmissionRouter router = new CoreRetransmissionRouter();
        router.registerRetransmissionService(REGION_CONNECTOR_ID_1, regionConnectorRetransmissionService);
        router.registerRetransmissionService(REGION_CONNECTOR_ID_2, regionConnectorRetransmissionService2);
        var now = LocalDate.now(ZoneOffset.UTC);

        router.registerRetransmissionConnector(new PlainRetransmissionOutboundConnector(testPublisher.flux()));
        router.registerRetransmissionConnector(new PlainRetransmissionOutboundConnector(testPublisher2.flux()));

        RetransmissionRequest request = new RetransmissionRequest(REGION_CONNECTOR_ID_1, PERMISSION_ID_1, now, now);
        RetransmissionRequest request2 = new RetransmissionRequest(REGION_CONNECTOR_ID_2, PERMISSION_ID_2, now, now);
        StepVerifier.Step<RetransmissionResult> stepVerifier = StepVerifier.create(router.retransmissionResults());

        // When
        testPublisher.next(request);
        testPublisher.next(request2);
        testPublisher.next(new RetransmissionRequest(REGION_CONNECTOR_ID_3, PERMISSION_ID_3, now, now));
        testPublisher2.next(request);
        router.close();


        // Then
        stepVerifier
                .expectNextMatches(result -> result instanceof Success(
                        String permissionId, ZonedDateTime ignored
                ) && permissionId.equals(PERMISSION_ID_1))
                .expectNextMatches(result -> result instanceof Success(
                        String permissionId, ZonedDateTime ignored
                ) && permissionId.equals(PERMISSION_ID_2))
                .expectNextMatches(result -> result instanceof RetransmissionServiceNotFound notFound &&
                                             notFound.regionConnectorId().equals(REGION_CONNECTOR_ID_3) &&
                                             notFound.permissionId().equals(PERMISSION_ID_3))
                .expectNextMatches(result -> result instanceof Success(
                        String permissionId, ZonedDateTime ignored
                ) && permissionId.equals(PERMISSION_ID_1))
                .verifyComplete();

        verify(regionConnectorRetransmissionService2, times(1)).requestRetransmission(request2);
        verify(regionConnectorRetransmissionService, times(2)).requestRetransmission(request);
    }

    @Test
    void subscribingToRetransmissionResultsMultipleTimes_doesNotInvokeServicesMultipleTimes() throws Exception {
        // Given
        TestPublisher<RetransmissionRequest> testPublisher = TestPublisher.create();
        CoreRetransmissionRouter router = new CoreRetransmissionRouter();
        router.registerRetransmissionService(REGION_CONNECTOR_ID_1, regionConnectorRetransmissionService);
        var now = LocalDate.now(ZoneOffset.UTC);

        router.registerRetransmissionConnector(new PlainRetransmissionOutboundConnector(testPublisher.flux()));

        RetransmissionRequest request = new RetransmissionRequest(REGION_CONNECTOR_ID_1, PERMISSION_ID_1, now, now);

        // When
        testPublisher.next(request);
        router.retransmissionResults().subscribe();
        router.retransmissionResults().subscribe();
        router.retransmissionResults().subscribe();
        router.retransmissionResults().subscribe();
        router.close();


        // Then
        verify(regionConnectorRetransmissionService, times(1)).requestRetransmission(request);
    }

    private record PlainRetransmissionOutboundConnector(
            Flux<RetransmissionRequest> retransmissionRequests) implements RetransmissionOutboundConnector {

        @Override
        public void setRetransmissionResultStream(Flux<RetransmissionResult> retransmissionResultStream) {
            // No-op
        }
    }
}
