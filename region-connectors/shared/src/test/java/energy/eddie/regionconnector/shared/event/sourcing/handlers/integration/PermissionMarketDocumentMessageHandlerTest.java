package energy.eddie.regionconnector.shared.event.sourcing.handlers.integration;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class PermissionMarketDocumentMessageHandlerTest {
    @Mock
    private PermissionRequestRepository<PermissionRequest> repository;
    @Mock
    private DataNeedsService dataNeedsService;

    @Test
    void testAccept_emitsPermissionMarketDocument() {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new SimplePermissionRequest(
                "pid", "cid", "dnid", start, end, ZonedDateTime.now(ZoneOffset.UTC), PermissionProcessStatus.VALIDATED
        );
        when(repository.findByPermissionId("pid")).thenReturn(Optional.of(permissionRequest));
        when(dataNeedsService.getById(anyString())).thenReturn(new AccountingPointDataNeed());
        PlainCommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "fallbackId");
        EventBus eventBus = new EventBusImpl();
        var handler = new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                                   repository,
                                                                   dataNeedsService,
                                                                   "EP-ID",
                                                                   cimConfig,
                                                                   pr -> null,
                                                                   ZoneOffset.UTC);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(handler.getPermissionMarketDocumentStream())
                    .then(handler::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testAccept_doesNotEmitStatus_ifNoPermissionIsFound() {
        // Given
        when(repository.findByPermissionId("pid")).thenReturn(Optional.empty());
        var cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "fallbackId");
        EventBus eventBus = new EventBusImpl();
        var handler = new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                                   repository,
                                                                   dataNeedsService,
                                                                   "EP-ID",
                                                                   cimConfig,
                                                                   pr -> null,
                                                                   ZoneOffset.UTC);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(handler.getPermissionMarketDocumentStream())
                    .then(handler::close)
                    .expectError(PermissionNotFoundException.class)
                    .verify();
    }

    @Test
    void testAccept_doesNotEmitStatus_onInternalEvent() {
        // Given
        var cimConfig = new PlainCommonInformationModelConfiguration(
                CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "fallbackId");
        EventBus eventBus = new EventBusImpl();
        var handler = new PermissionMarketDocumentMessageHandler<>(eventBus,
                                                                   repository,
                                                                   dataNeedsService,
                                                                   "EP-ID",
                                                                   cimConfig,
                                                                   pr -> null,
                                                                   ZoneOffset.UTC);

        // When
        eventBus.emit(new InternalEvent("pid", PermissionProcessStatus.VALIDATED));

        // Then
        StepVerifier.create(handler.getPermissionMarketDocumentStream())
                    .then(handler::close)
                    .verifyComplete();
    }
}
