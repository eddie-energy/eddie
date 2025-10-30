package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.CodeboekApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoints;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.exceptions.NlValidationException;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthRequestPayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.*;
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
import reactor.core.publisher.Flux;

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

    @Mock
    private OAuthManager oAuthManager;
    @Mock
    private Outbox outbox;
    @Mock
    private CodeboekApiClient codeboekApiClient;
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
    private ArgumentCaptor<NlAccountingPointValidatedEvent> accountingPointValidatedCaptor;
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
    void testCreatePermissionRequest_forValidatedHistoricalData_emitsCreatedAndValidatedEvent() throws DataNeedNotFoundException, UnsupportedDataNeedException, NlValidationException {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        var now = LocalDate.now(NL_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.P1D),
                        new Timeframe(now.minusDays(-10), now.minusDays(-1)),
                        new Timeframe(now, now)
                ));
        when(oAuthManager.createAuthorizationUrl(any()))
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
    void testCreatePermissionRequest_forAccountingPointData_emitsCreatedAndValidatedEvent() throws DataNeedNotFoundException, UnsupportedDataNeedException, NlValidationException {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01", "9999AB");
        var now = LocalDate.now(NL_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(now, now)));
        when(oAuthManager.createAuthorizationUrl(any()))
                .thenReturn(new OAuthRequestPayload(URI.create(""), "state", "codeVerifier"));
        var data = new MeteringPoints().meteringPoints(List.of(new MeteringPoint()));
        when(codeboekApiClient.meteringPoints("9999AB", "01"))
                .thenReturn(Flux.just(data));

        // When
        permissionRequestService.createPermissionRequest(permissionRequest);

        // Then
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(accountingPointValidatedCaptor.capture());
        var created = createdCaptor.getValue();
        var validated = accountingPointValidatedCaptor.getValue();
        assertAll(
                () -> assertEquals("cid", created.connectionId()),
                () -> assertEquals("dnid", created.dataNeedId()),
                () -> assertEquals(now, validated.start()),
                () -> assertEquals(now, validated.end()),
                () -> assertEquals("01", validated.houseNumber()),
                () -> assertEquals("9999AB", validated.postalCode())
        );
    }

    @Test
    void testCreatePermissionRequest_forAccountingPointData_withInvalidPayload_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01", null);
        var now = LocalDate.now(NL_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(now, now)));

        // When & Then
        assertThrows(NlValidationException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(isA(NlMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_forAccountingPointData_withoutMeter_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01", "9999AB");
        var now = LocalDate.now(NL_ZONE_ID);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(now, now)));
        var data = new MeteringPoints();
        when(codeboekApiClient.meteringPoints("9999AB", "01")).thenReturn(Flux.just(data));

        // When & Then
        assertThrows(NlValidationException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(createdCaptor.capture());
        verify(outbox).commit(isA(NlMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_withInvalidDataNeed_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotSupportedResult(""));

        // When & Then
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
        when(oAuthManager.processCallback(any(), any()))
                .thenReturn("pid");
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(createPermissionRequest()));

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
        when(oAuthManager.processCallback(any(), any()))
                .thenThrow(clazz);
        when(permissionRequestRepository.findByPermissionId("pid"))
                .thenReturn(Optional.of(createPermissionRequest()));

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

    private static MijnAansluitingPermissionRequest createPermissionRequest() {
        return new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                "state",
                "verifier",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.PT15M,
                "11",
                "999AB"
        );
    }
}