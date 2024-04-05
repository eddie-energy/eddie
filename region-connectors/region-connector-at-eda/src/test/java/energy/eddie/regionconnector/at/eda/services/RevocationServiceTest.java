package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaCMRevoke;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevocationServiceTest {
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<SimpleEvent> captor;

    @Test
    void revokeWithConsentId_revokesPermissionRequest() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var permissionRequest = new EdaPermissionRequest("cid", "dnid", mock(CCMORequest.class), Granularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, null, null);
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.of(permissionRequest));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke().setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.REVOKED, captor.getValue().status());
    }

    @Test
    void revokeWithoutConsentId_revokesPermissionRequest() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        when(ccmoRequest.start()).thenReturn(now);
        when(ccmoRequest.end()).thenReturn(Optional.of(now.plusDays(10)));
        var permissionRequest = new EdaPermissionRequest("cid", "dnid", ccmoRequest, Granularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, null, null);
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.empty());

        when(repository.findAcceptedAndFulfilledByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(permissionRequest));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke()
                .setConsentEnd(now)
                .setMeteringPoint("mpid")
                .setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.REVOKED, captor.getValue().status());
    }

    @Test
    void revokeWithWrongPermissionState_doesNotRevokePermissionRequest() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        when(ccmoRequest.start()).thenReturn(now);
        when(ccmoRequest.end()).thenReturn(Optional.of(now.plusDays(10)));
        var permissionRequest = new EdaPermissionRequest("cid", "dnid", ccmoRequest, Granularity.PT15M,
                                                         PermissionProcessStatus.CREATED, null, null);
        TestPublisher<EdaCMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var repository = mock(AtPermissionRequestRepository.class);
        when(repository.findByConsentId("consentId")).thenReturn(Optional.empty());

        when(repository.findAcceptedAndFulfilledByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(permissionRequest));
        EdaCMRevoke cmRevoke = new SimpleEdaCMRevoke()
                .setConsentEnd(now)
                .setMeteringPoint("mpid")
                .setConsentId("consentId");
        new RevocationService(edaAdapter, repository, outbox);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        verify(outbox, never()).commit(any());
    }
}
