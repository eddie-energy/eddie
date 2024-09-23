package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthRequestPayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.MijnAansluitingDataSourceInformation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlCreatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlMalformedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlValidatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.NL_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    public static final MijnAansluitingPermissionRequest PERMISSION_REQUEST = new MijnAansluitingPermissionRequest("pid",
                                                                                                                   "cid",
                                                                                                                   "dnid",
                                                                                                                   PermissionProcessStatus.VALIDATED,
                                                                                                                   "state",
                                                                                                                   "verifier",
                                                                                                                   ZonedDateTime.now(
                                                                                                                           ZoneOffset.UTC),
                                                                                                                   LocalDate.now(
                                                                                                                           ZoneOffset.UTC),
                                                                                                                   LocalDate.now(
                                                                                                                           ZoneOffset.UTC),
                                                                                                                   Granularity.PT15M);
    @Mock
    private OAuthManager oAuthManager;
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private NlPermissionRequestRepository permissionRequestRepository;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private PermissionRequestService permissionRequestService;
    @Captor
    private ArgumentCaptor<NlCreatedEvent> createdCaptor;
    @Captor
    private ArgumentCaptor<NlValidatedEvent> validatedCaptor;
    @Captor
    private ArgumentCaptor<NlSimpleEvent> simpleCaptor;

    public static Stream<Arguments> testReceiveResponse_exceptionCase_returnsUnhappyResponse() {
        return Stream.of(
                Arguments.of(UserDeniedAuthorizationException.class, PermissionProcessStatus.REJECTED, 2),
                Arguments.of(OAuthException.class, PermissionProcessStatus.INVALID, 2),
                Arguments.of(JWTSignatureCreationException.class, PermissionProcessStatus.UNABLE_TO_SEND, 1)
        );
    }

    @Test
    void testCreatePermissionRequest_forValidatedHistoricalData_emitsCreatedAndValidatedEvent() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        var now = LocalDate.now(NL_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.P1D),
                        new Timeframe(now.minusDays(-10), now.minusDays(-1)),
                        new Timeframe(now, now)
                ));
        when(oAuthManager.createAuthorizationUrl(any(), any()))
                .thenReturn(new OAuthRequestPayload(URI.create(""), "state", "codeVerifier"));

        // When
        permissionRequestService.createPermissionRequest(permissionRequest);

        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        var created = createdCaptor.getValue();
        var validated = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals("dnid", created.dataNeedId()),
                () -> assertEquals(Granularity.P1D, validated.granularity())
        );
    }

    @Test
    void testCreatePermissionRequest_forAccountingPointData_emitsCreatedAndValidatedEvent() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        var now = LocalDate.now(NL_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(now, now)));
        when(oAuthManager.createAuthorizationUrl(any(), any()))
                .thenReturn(new OAuthRequestPayload(URI.create(""), "state", "codeVerifier"));

        // When
        permissionRequestService.createPermissionRequest(permissionRequest);

        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(validatedCaptor.capture());
        var created = createdCaptor.getValue();
        var validated = validatedCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals("dnid", created.dataNeedId()),
                () -> assertEquals(now, validated.start()),
                () -> assertEquals(now, validated.end()),
                () -> assertNull(validated.granularity())
        );
    }

    @Test
    void testCreatePermissionRequest_withInvalidDataNeed_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotSupportedResult(""));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(isA(NlMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_withUnknownDataNeed_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotFoundResult());

        // When
        // Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(isA(NlMalformedEvent.class));
    }

    @Test
    void testReceiveResponse_acceptedResponse_returnsAccepted() throws UserDeniedAuthorizationException, JWTSignatureCreationException, OAuthException, ParseException, PermissionNotFoundException, InvalidValidationAddressException, IllegalTokenException, OAuthUnavailableException {
        // Given
        when(oAuthManager.processCallback(any(), any(), any()))
                .thenReturn("pid");
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(PERMISSION_REQUEST));
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new AccountingPointDataNeed());

        // When
        var res = permissionRequestService.receiveResponse(URI.create(""), "pid");

        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, res);
        verify(outbox, times(2)).commit(simpleCaptor.capture());
        assertEquals(PermissionProcessStatus.ACCEPTED, simpleCaptor.getValue().status());
    }

    @ParameterizedTest
    @MethodSource
    void testReceiveResponse_exceptionCase_returnsUnhappyResponse(
            Class<Exception> clazz,
            PermissionProcessStatus status,
            int times
    ) throws UserDeniedAuthorizationException, JWTSignatureCreationException, OAuthException, ParseException, PermissionNotFoundException, InvalidValidationAddressException, IllegalTokenException, OAuthUnavailableException {
        // Given
        when(oAuthManager.processCallback(any(), any(), any()))
                .thenThrow(clazz);
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(PERMISSION_REQUEST));

        when(dataNeedsService.getById("dnid"))
                .thenReturn(new AccountingPointDataNeed());

        // When
        var res = permissionRequestService.receiveResponse(URI.create(""), "pid");

        // Then
        assertEquals(status, res);
        verify(outbox, times(times)).commit(simpleCaptor.capture());
        assertEquals(status, simpleCaptor.getValue().status());
    }

    @Test
    void testReceiveResponse_withUnknownPermissionRequest_throwsPermissionNotFound() {
        // Given
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When
        // Then
        assertThrows(PermissionNotFoundException.class,
                     () -> permissionRequestService.receiveResponse(URI.create(""), "pid"));
    }

    @Test
    void testConnectionStatusMessage_throwsOnUnknownPermissionRequest() {
        // Given
        when(permissionRequestRepository.findByPermissionId(any()))
                .thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class,
                     () -> permissionRequestService.connectionStatusMessage("pid"));
    }

    @Test
    void testConnectionStatusMessage_returnsMessage() throws PermissionNotFoundException {
        // Given
        var pr = new MijnAansluitingPermissionRequest(
                "pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED,
                null, null, null, null, null, null
        );
        when(permissionRequestRepository.findByPermissionId(any()))
                .thenReturn(Optional.of(pr));

        // When
        var res = permissionRequestService.connectionStatusMessage("pid");

        // Then
        assertAll(
                () -> assertEquals("cid", res.connectionId()),
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals("dnid", res.dataNeedId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, res.status()),
                () -> assertEquals(new MijnAansluitingDataSourceInformation(), res.dataSourceInformation())
        );
    }
}