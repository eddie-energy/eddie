package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fi.fingrid.FingridRegionConnectorMetadata;
import energy.eddie.regionconnector.fi.fingrid.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fi.fingrid.permission.events.CreatedEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.fi.fingrid.persistence.FiPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionCreationServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    @Mock
    private FiPermissionRequestRepository permissionRequestRepository;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private PermissionCreationService permissionCreationService;
    @Captor
    private ArgumentCaptor<CreatedEvent> createdCaptor;
    @Captor
    private ArgumentCaptor<ValidatedEvent> validatedCaptor;
    @Captor
    private ArgumentCaptor<SimpleEvent> simpleCaptor;

    public static Stream<Arguments> createAndValidatePermissionRequest_throwsOnUnsupportedDataNeed() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(today, today);
        return Stream.of(
                Arguments.of(new DataNeedNotSupportedResult("")),
                Arguments.of(new AccountingPointDataNeedResult(timeframe))
        );
    }

    @Test
    void createAndValidatePermissionRequest_createsPermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        // Given
        var forCreation = new PermissionRequestForCreation("cid", "dnid", "identifier");
        var today = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedCalculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT1H),
                        new Timeframe(today, today),
                        new Timeframe(today.minusDays(10), today.minusDays(1))
                ));
        when(jwtUtil.createJwt(eq(FingridRegionConnectorMetadata.REGION_CONNECTOR_ID), anyString())).thenReturn("");

        // When
        var res = permissionCreationService.createAndValidatePermissionRequest(forCreation);

        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        assertAll(
                () -> assertEquals(res.permissionId(), createdCaptor.getValue().permissionId()),
                () -> assertEquals(res.permissionId(), validatedCaptor.getValue().permissionId())
        );
    }

    @Test
    void createAndValidatePermissionRequest_throwsOnUnknownDataNeed() {
        // Given
        var forCreation = new PermissionRequestForCreation("cid", "dnid", "identifier");
        when(dataNeedCalculationService.calculate("dnid")).thenReturn(new DataNeedNotFoundResult());

        // When, Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> permissionCreationService.createAndValidatePermissionRequest(forCreation));
    }

    @ParameterizedTest
    @MethodSource
    void createAndValidatePermissionRequest_throwsOnUnsupportedDataNeed(DataNeedCalculationResult calculation) {
        // Given
        var forCreation = new PermissionRequestForCreation("cid", "dnid", "identifier");
        when(dataNeedCalculationService.calculate("dnid"))
                .thenReturn(calculation);

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionCreationService.createAndValidatePermissionRequest(forCreation));

        verify(outbox).commit(createdCaptor.capture());
    }

    @Test
    void acceptOrReject_throwsOnUnsupportedStatus() {
        // Given
        var invalidState = PermissionProcessStatus.CREATED;

        // When, Then
        assertThrows(IllegalArgumentException.class,
                     () -> permissionCreationService.acceptOrReject("pid", invalidState));
    }

    @Test
    void acceptOrReject_throwsOnPermissionRequestWithInvalidState() {
        // Given
        var invalidState = PermissionProcessStatus.CREATED;
        var today = LocalDate.now(ZoneOffset.UTC);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                invalidState,
                ZonedDateTime.now(ZoneOffset.UTC),
                today,
                today,
                "identifier"
        );
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When, Then
        assertThrows(PermissionStateTransitionException.class,
                     () -> permissionCreationService.acceptOrReject("pid", PermissionProcessStatus.ACCEPTED));
    }

    @Test
    void acceptOrReject_throwsOnUnknownPermissionRequest() {
        // Given
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class,
                     () -> permissionCreationService.acceptOrReject("pid", PermissionProcessStatus.ACCEPTED));
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"ACCEPTED", "REJECTED"})
    void acceptOrReject_acceptsPermissionRequest(PermissionProcessStatus status) throws PermissionNotFoundException, PermissionStateTransitionException {
        // Given
        var today = LocalDate.now(ZoneOffset.UTC);
        var pr = new FingridPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                ZonedDateTime.now(ZoneOffset.UTC),
                today,
                today,
                "identifier"
        );
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        permissionCreationService.acceptOrReject("pid", status);

        // Then
        verify(outbox, times(2)).commit(simpleCaptor.capture());
        assertAll(
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   simpleCaptor.getAllValues().getFirst().status()),
                () -> assertEquals(status, simpleCaptor.getValue().status())
        );
    }
}
