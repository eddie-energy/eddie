package energy.eddie.core.services;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.RetransmissionServiceNotFound;
import energy.eddie.api.agnostic.retransmission.result.Success;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreRetransmissionRouterTest {
    @Spy
    private RegionConnectorRetransmissionService regionConnectorRetransmissionService;
    @Spy
    private RegionConnectorRetransmissionService regionConnectorRetransmissionService2;

    @Test
    void routeRetransmissionRequest_routesToRegisteredServices() {
        // Given
        when(regionConnectorRetransmissionService.requestRetransmission(any())).thenReturn(Mono.just(new Success()));
        when(regionConnectorRetransmissionService2.requestRetransmission(any())).thenReturn(Mono.just(new Success()));

        CoreRetransmissionRouter router = new CoreRetransmissionRouter();
        router.registerRetransmissionService("s1", regionConnectorRetransmissionService);
        router.registerRetransmissionService("s2", regionConnectorRetransmissionService2);
        RetransmissionRequest request = new RetransmissionRequest("permissionId", LocalDate.now(), LocalDate.now());
        // When
        router.routeRetransmissionRequest("s1", request).subscribe();
        var notFoundVerifier = StepVerifier.create(router.routeRetransmissionRequest("s3", request));
        router.routeRetransmissionRequest("s1", request).subscribe();
        router.routeRetransmissionRequest("s2", request).subscribe();

        // Then
        verify(regionConnectorRetransmissionService, times(2)).requestRetransmission(request);
        verify(regionConnectorRetransmissionService2, times(1)).requestRetransmission(request);
        notFoundVerifier
                .expectError(RetransmissionServiceNotFound.class)
                .verify(Duration.ofMillis(100));
    }

    @Test
    void router_handlesServiceErrors() {
        // Given
        when(regionConnectorRetransmissionService.requestRetransmission(any()))
                .thenReturn(Mono.error(new RuntimeException("error")))
                .thenReturn(Mono.just(new Success()));

        CoreRetransmissionRouter router = new CoreRetransmissionRouter();
        router.registerRetransmissionService("s1", regionConnectorRetransmissionService);
        RetransmissionRequest request = new RetransmissionRequest("permissionId", LocalDate.now(), LocalDate.now());

        // When
        router.routeRetransmissionRequest("s1", request).subscribe();
        router.routeRetransmissionRequest("s1", request).subscribe();
        router.routeRetransmissionRequest("s1", request).subscribe();

        // Then
        verify(regionConnectorRetransmissionService, times(3)).requestRetransmission(request);
    }
}
