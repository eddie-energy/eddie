package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
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
import java.time.Period;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Captor
    ArgumentCaptor<NlSimpleEvent> simpleCaptor;
    @Mock
    private OAuthManager oAuthManager;
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed vhdDataNeed;
    @Mock
    private AiidaDataNeed aiidaDataNeed;
    @Mock
    private NlPermissionRequestRepository permissionRequestRepository;
    @Mock
    private RelativeDuration dataNeedDuration;
    @InjectMocks
    private PermissionRequestService permissionRequestService;
    @Captor
    private ArgumentCaptor<NlMalformedEvent> malformedCaptor;
    @Captor
    private ArgumentCaptor<NlCreatedEvent> createdCaptor;
    @Captor
    private ArgumentCaptor<NlValidatedEvent> validatedCaptor;

    public static Stream<Arguments> testReceiveResponse_exceptionCase_returnsUnhappyResponse() {
        return Stream.of(
                Arguments.of(UserDeniedAuthorizationException.class, PermissionProcessStatus.REJECTED, 2),
                Arguments.of(OAuthException.class, PermissionProcessStatus.INVALID, 2),
                Arguments.of(JWTSignatureCreationException.class, PermissionProcessStatus.UNABLE_TO_SEND, 1)
        );
    }

    @Test
    void testCreatePermissionRequest_emitsCreatedAndValidatedEvent() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.P1D);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1D);
        when(vhdDataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);
        when(vhdDataNeed.duration())
                .thenReturn(dataNeedDuration);
        when(dataNeedDuration.start())
                .thenReturn(Optional.of(Period.ofDays(-10)));
        when(dataNeedDuration.end())
                .thenReturn(Optional.of(Period.ofDays(-1)));
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
    void testCreatePermissionRequest_withInvalidDataNeed_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.empty());

        // When
        // Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(isA(NlMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_withInvalidGranularity_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.PT15M);
        when(vhdDataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(malformedCaptor.capture());
        var res = malformedCaptor.getValue();
        assertThat(res.errors()).hasSize(1);
    }

    @Test
    void testCreatePermissionRequest_withInvalidStartDate_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.minGranularity())
                .thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity())
                .thenReturn(Granularity.P1D);
        when(vhdDataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);
        when(vhdDataNeed.duration())
                .thenReturn(dataNeedDuration);
        when(dataNeedDuration.start())
                .thenReturn(Optional.of(Period.ofYears(-100)));
        when(dataNeedDuration.end())
                .thenReturn(Optional.of(Period.ofDays(-1)));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(malformedCaptor.capture());
        var res = malformedCaptor.getValue();
        assertThat(res.errors()).hasSize(1);
    }

    @Test
    void testCreatePermissionRequest_withInvalidEnergyType_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.energyType())
                .thenReturn(EnergyType.HEAT);

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(malformedCaptor.capture());
        var res = malformedCaptor.getValue();
        assertThat(res.errors()).hasSize(1);
    }

    @Test
    void testCreatePermissionRequest_withInvalidDataNeedType_throws() {
        // Given
        var permissionRequest = new PermissionRequestForCreation("cid", "dnid", "01");
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(aiidaDataNeed));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(permissionRequest));
        verify(outbox).commit(isA(NlCreatedEvent.class));
        verify(outbox).commit(malformedCaptor.capture());
        var res = malformedCaptor.getValue();
        assertThat(res.errors()).hasSize(1);
    }

    @Test
    void testReceiveResponse_acceptedResponse_returnsAccepted() throws UserDeniedAuthorizationException, JWTSignatureCreationException, OAuthException, ParseException, PermissionNotFoundException, InvalidValidationAddressException, IllegalTokenException, OAuthUnavailableException {
        // Given
        when(oAuthManager.processCallback(any(), any()))
                .thenReturn("pid");

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

        // When
        var res = permissionRequestService.receiveResponse(URI.create(""), "pid");

        // Then
        assertEquals(status, res);
        verify(outbox, times(times)).commit(simpleCaptor.capture());
        assertEquals(status, simpleCaptor.getValue().status());
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