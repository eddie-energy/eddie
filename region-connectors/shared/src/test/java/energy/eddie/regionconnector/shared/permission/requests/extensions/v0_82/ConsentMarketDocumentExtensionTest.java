package energy.eddie.regionconnector.shared.permission.requests.extensions.v0_82;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsentMarketDocumentExtensionTest {

    @Test
    void accept_emitsConsentMarketDocument() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.minusDays(10);
        ZonedDateTime end = now.minusDays(5);

        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.countryCode()).thenReturn("AT");
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("paID");
        when(dataSourceInformation.regionConnectorId()).thenReturn("rc");


        var permissionRequest = mock(TimeframedPermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid", "pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.created()).thenReturn(now);
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        when(permissionRequest.start()).thenReturn(start);
        when(permissionRequest.end()).thenReturn(end);
        when(permissionRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        Sinks.Many<ConsentMarketDocument> sink = Sinks.many().multicast().onBackpressureBuffer();
        var extension = new ConsentMarketDocumentExtension<>(sink, "customerId", "NAT");

        // When
        extension.accept(permissionRequest);

        // Then
        StepVerifier.create(sink.asFlux())
                .then(sink::tryEmitComplete)
                .assertNext(cmd -> assertEquals("pid", cmd.getMRID()))
                .verifyComplete();
    }

    @Test
    void accept_emitsErrorOnException() {
        // Given
        var permissionRequest = mock(TimeframedPermissionRequest.class);
        when(permissionRequest.permissionId()).thenThrow(new RuntimeException());

        Sinks.Many<ConsentMarketDocument> sink = Sinks.many().multicast().onBackpressureBuffer();
        var extension = new ConsentMarketDocumentExtension<>(sink, "customerId", "NAT");

        // When
        extension.accept(permissionRequest);

        // Then
        StepVerifier.create(sink.asFlux())
                .then(sink::tryEmitComplete)
                .expectError(RuntimeException.class)
                .verify();
    }

}