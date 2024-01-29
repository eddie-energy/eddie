package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import reactor.test.publisher.TestPublisher;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AcceptedPermissionRequestStateTest {

    @Test
    void validate_throws() {
        // Given
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_transitionsToTerminatedState() {
        // Given
        AtConfiguration configuration = new PlainAtConfiguration("ep", null);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.createCold();
        CMRequestStatus cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "", "convId");
        cmRequestStatus.setCmConsentId("consentId");
        var edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.dsoId()).thenReturn("dsoId");
        var factory = new StateBuilderFactory(configuration, edaAdapter);
        var permissionRequest = new EdaPermissionRequest("cid", "pid", "dnid", ccmoRequest, factory);
        permissionRequest.setConsentId("consentId");
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(permissionRequest, edaAdapter, configuration, factory);
        testPublisher.emit(cmRequestStatus);

        // When
        state.terminate();

        // Then
        assertInstanceOf(AtTerminatedPermissionRequestState.class, permissionRequest.state());
    }

    @Test
    void terminate_doesNotTransitionOnError() throws TransmissionException, JAXBException {
        // Given
        AtConfiguration configuration = new PlainAtConfiguration("ep", null);

        var edaAdapter = mock(EdaAdapter.class);
        doThrow(new TransmissionException(new RuntimeException())).when(edaAdapter).sendCMRevoke(any());

        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.permissionAdministratorId()).thenReturn("dsoId");
        var permissionRequest = mock(AtPermissionRequest.class);
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        var factory = new StateBuilderFactory(configuration, edaAdapter);
        AtAcceptedPermissionRequestState state = new AtAcceptedPermissionRequestState(permissionRequest, edaAdapter, configuration, factory);

        // When
        state.terminate();

        // Then
        verify(permissionRequest, never()).changeState(ArgumentMatchers.any());
    }
}