package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.ProcessDirectory;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import reactor.test.publisher.TestPublisher;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevocationServiceTest {

    @Test
    void revokeWithConsentId_revokesPermissionRequest() {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var permissionRequest = new EdaPermissionRequest("cid", "dnid", mock(CCMORequest.class), edaAdapter);
        permissionRequest.changeState(new AtAcceptedPermissionRequestState(permissionRequest));
        TestPublisher<CMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByConsentId("consentId")).thenReturn(Optional.of(permissionRequest));
        CMRevoke cmRevoke = new CMRevoke();
        cmRevoke.setProcessDirectory(new ProcessDirectory().withConsentId("consentId"));
        new RevocationService(edaAdapter, permissionRequestService);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());
    }

    @Test
    void revokeWithoutConsentId_revokesPermissionRequest() throws DatatypeConfigurationException {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        when(ccmoRequest.dataFrom()).thenReturn(now);
        when(ccmoRequest.dataTo()).thenReturn(Optional.of(now.plusDays(10)));
        var permissionRequest = new EdaPermissionRequest("cid", "dnid", ccmoRequest, edaAdapter);
        permissionRequest.setMeteringPointId("mpid");
        permissionRequest.changeState(new AtAcceptedPermissionRequestState(permissionRequest));
        TestPublisher<CMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByConsentId("consentId")).thenReturn(Optional.empty());

        when(permissionRequestService.findByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRevoke cmRevoke = new CMRevoke();
        cmRevoke.setProcessDirectory(
                new ProcessDirectory()
                        .withConsentId("consentId")
                        .withConsentEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar(now.toString()))
                        .withMeteringPoint("mpid")
        );
        new RevocationService(edaAdapter, permissionRequestService);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        assertEquals(PermissionProcessStatus.REVOKED, permissionRequest.state().status());
    }

    @Test
    void revokeWithWrongPermissionState_doesNotRevokePermissionRequest() throws DatatypeConfigurationException {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        when(ccmoRequest.dataFrom()).thenReturn(now);
        when(ccmoRequest.dataTo()).thenReturn(Optional.of(now.plusDays(10)));
        var permissionRequest = new EdaPermissionRequest("cid", "dnid", ccmoRequest, edaAdapter);
        permissionRequest.setMeteringPointId("mpid");
        TestPublisher<CMRevoke> revocationStream = TestPublisher.create();
        when(edaAdapter.getCMRevokeStream()).thenReturn(revocationStream.flux());
        var permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByConsentId("consentId")).thenReturn(Optional.empty());

        when(permissionRequestService.findByMeteringPointIdAndDate(anyString(), any()))
                .thenReturn(List.of(permissionRequest));
        CMRevoke cmRevoke = new CMRevoke();
        cmRevoke.setProcessDirectory(
                new ProcessDirectory()
                        .withConsentId("consentId")
                        .withConsentEnd(DatatypeFactory.newInstance().newXMLGregorianCalendar(now.toString()))
                        .withMeteringPoint("mpid")
        );
        new RevocationService(edaAdapter, permissionRequestService);

        // When
        revocationStream.emit(cmRevoke);

        // Then
        assertEquals(PermissionProcessStatus.CREATED, permissionRequest.state().status());
    }
}