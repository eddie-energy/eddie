package energy.eddie.regionconnector.at.eda.handlers.integration.outbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentMarketDocumentMessageHandlerTest {
    @Mock
    private AtPermissionRequestRepository repository;

    @Test
    void testAccept_emitsConsentMarketDocument() {
        // Given
        Sinks.Many<ConsentMarketDocument> messages = Sinks.many().multicast().onBackpressureBuffer();
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        EdaPermissionRequest permissionRequest = new EdaPermissionRequest(
                "connectionId", "pid", "dnid", "cmRequestId", "conversationId", "mid", "dsoId", start, end,
                Granularity.PT15M, PermissionProcessStatus.VALIDATED, "", null,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        PlainAtConfiguration atConfig = new PlainAtConfiguration("epId", null);
        PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        EventBus eventBus = new EventBusImpl();
        new ConsentMarketDocumentMessageHandler(eventBus, repository, messages, atConfig,
                                                cimConfig);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(messages.asFlux())
                .then(messages::tryEmitComplete)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testAccept_doesNotEmitStatus_ifNoPermissionIsFound() {
        // Given
        Sinks.Many<ConsentMarketDocument> messages = Sinks.many().multicast().onBackpressureBuffer();
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());
        PlainAtConfiguration atConfig = new PlainAtConfiguration("epId", null);
        PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        EventBus eventBus = new EventBusImpl();
        new ConsentMarketDocumentMessageHandler(eventBus, repository, messages, atConfig,
                                                cimConfig);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(messages.asFlux())
                .then(messages::tryEmitComplete)
                .expectError(PermissionNotFoundException.class)
                .verify();
    }
}