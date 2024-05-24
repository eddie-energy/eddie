package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEventForVHD;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedVHDHandlerTest {
    @Mock
    private Outbox outbox;
    @Mock
    private HistoricalDataService historicalDataService;
    @Mock
    private EsPermissionRequestRepository repository;
    @Spy
    private EventBus eventBus = new EventBusImpl();


    @InjectMocks
    @SuppressWarnings("unused")
    private AcceptedVHDHandler acceptedVHDHandler;

    private static Stream<Arguments> unfulfillableGranularities() {
        return Stream.of(
                Arguments.of(AllowedGranularity.PT15M, 3),
                Arguments.of(AllowedGranularity.PT15M, 4)
        );
    }

    private static Stream<Arguments> fulfillableGranularities() {
        return Stream.of(
                Arguments.of(AllowedGranularity.PT15M, 1, Granularity.PT15M),
                Arguments.of(AllowedGranularity.PT15M, 2, Granularity.PT15M),
                Arguments.of(AllowedGranularity.PT15M_OR_PT1H, 1, Granularity.PT15M),
                Arguments.of(AllowedGranularity.PT15M_OR_PT1H, 2, Granularity.PT15M),
                Arguments.of(AllowedGranularity.PT15M_OR_PT1H, 3, Granularity.PT1H),
                Arguments.of(AllowedGranularity.PT15M_OR_PT1H, 4, Granularity.PT1H),
                Arguments.of(AllowedGranularity.PT1H, 1, Granularity.PT1H),
                Arguments.of(AllowedGranularity.PT1H, 2, Granularity.PT1H),
                Arguments.of(AllowedGranularity.PT1H, 3, Granularity.PT1H),
                Arguments.of(AllowedGranularity.PT1H, 4, Granularity.PT1H)
        );
    }

    @Test
    void testAccept_withUnknownPermissionRequest_doesNothing() {
        // Given
        doThrow(EntityNotFoundException.class).when(repository).getByPermissionId("pid");

        // When
        eventBus.emit(new EsAcceptedEventForVHD("pid", DistributorCode.VIESGO, 4, true));

        // Then
        verifyNoInteractions(outbox);
    }

    @ParameterizedTest
    @MethodSource("unfulfillableGranularities")
    void testAccept_withUnfulfillableGranularity_commitsUnfulfillableEvent(
            AllowedGranularity allowedGranularity,
            int supplyPointType
    ) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                Granularity.PT1H,
                "nif",
                "mid",
                now,
                now.plusDays(10),
                null,
                null,
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC),
                allowedGranularity
        );
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        eventBus.emit(new EsAcceptedEventForVHD("pid", DistributorCode.VIESGO, supplyPointType, false));

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(event.status(), event.status())
        )));
        verifyNoInteractions(historicalDataService);
    }

    @ParameterizedTest
    @MethodSource("fulfillableGranularities")
    void testAcceptValidatedHistoricalDataNeed_requestsData(
            AllowedGranularity allowedGranularity,
            int supplyPointType,
            Granularity expectedGranularity
    ) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                Granularity.PT1H,
                "nif",
                "mid",
                now,
                now.plusDays(10),
                null,
                supplyPointType,
                null,
                PermissionProcessStatus.ACCEPTED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC),
                allowedGranularity
        );
        when(repository.getByPermissionId("pid")).thenReturn(pr);

        // When
        eventBus.emit(new EsAcceptedEventForVHD("pid", DistributorCode.VIESGO, supplyPointType, true));

        // Then
        verify(historicalDataService).fetchAvailableHistoricalData(assertArg(permissionRequest -> assertAll(
                () -> assertEquals(expectedGranularity, permissionRequest.granularity()),
                () -> assertEquals(Optional.of(DistributorCode.VIESGO), permissionRequest.distributorCode()),
                () -> assertEquals(Optional.of(supplyPointType), permissionRequest.pointType()),
                () -> assertTrue(permissionRequest.productionSupport())
        )));
    }
}
