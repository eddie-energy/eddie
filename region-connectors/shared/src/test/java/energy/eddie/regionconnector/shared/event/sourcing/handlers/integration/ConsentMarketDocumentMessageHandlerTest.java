package energy.eddie.regionconnector.shared.event.sourcing.handlers.integration;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.states.ValidatedPermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.permission.requests.extensions.SimplePermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentMarketDocumentMessageHandlerTest {
    @Mock
    private PermissionRequestRepository<PermissionRequest> repository;
    @Mock
    private ValidatedPermissionRequestState state;

    @Test
    void testAccept_emitsConsentMarketDocument() {
        // Given
        Sinks.Many<ConsentMarketDocument> messages = Sinks.many().multicast().onBackpressureBuffer();
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new SimplePermissionRequest(
                "pid", "cid", state, "dnid", start, end, ZonedDateTime.now(ZoneOffset.UTC)
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        EventBus eventBus = new EventBusImpl();
        new ConsentMarketDocumentMessageHandler<>(eventBus,
                                                  repository,
                                                  messages,
                                                  "EP-ID",
                                                  cimConfig,
                                                  pr -> null,
                                                  ZoneOffset.UTC);

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
        PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        EventBus eventBus = new EventBusImpl();
        new ConsentMarketDocumentMessageHandler<>(eventBus,
                                                  repository,
                                                  messages,
                                                  "EP-ID",
                                                  cimConfig,
                                                  pr -> null,
                                                  ZoneOffset.UTC);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(messages.asFlux())
                    .then(messages::tryEmitComplete)
                    .expectError(PermissionNotFoundException.class)
                    .verify();
    }

    @Test
    void testAccept_doesNotEmitStatus_onInternalEvent() {
        // Given
        Sinks.Many<ConsentMarketDocument> messages = Sinks.many().multicast().onBackpressureBuffer();
        PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME);
        EventBus eventBus = new EventBusImpl();
        new ConsentMarketDocumentMessageHandler<>(eventBus,
                                                  repository,
                                                  messages,
                                                  "EP-ID",
                                                  cimConfig,
                                                  pr -> null,
                                                  ZoneOffset.UTC);

        // When
        eventBus.emit(new InternalEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(messages.asFlux())
                    .then(messages::tryEmitComplete)
                    .verifyComplete();
    }
}
