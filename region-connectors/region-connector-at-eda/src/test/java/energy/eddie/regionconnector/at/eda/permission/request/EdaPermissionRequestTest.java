package energy.eddie.regionconnector.at.eda.permission.request;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.states.*;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;
import org.junit.jupiter.api.Test;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaPermissionRequestTest {

    @Test
    void edaPermissionRequest_hasCreatedStateAsInitialState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("cid", "dataNeedId", ccmoRequest, factory);

        // When
        var state = permissionRequest.state();

        // Then
        assertEquals(AtCreatedPermissionRequestState.class, state.getClass());
    }

    @Test
    void edaPermissionRequest_changesState() {
        // Given
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        AtCreatedPermissionRequestState createdState = new AtCreatedPermissionRequestState(null, null, factory);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("cid", "dataNeedId", ccmoRequest, factory);

        // When
        permissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, permissionRequest.state());
    }

    @Test
    void edaPermissionRequest_returnsCMRequestId() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);

        // When
        String cmRequestId = permissionRequest.cmRequestId();

        // Then
        assertEquals("cmRequestId", cmRequestId);
    }

    @Test
    void messagingPermissionRequest_returnsConversationId() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);

        // When
        String conversationId = permissionRequest.conversationId();

        // Then
        assertEquals("messageId", conversationId);
    }

    @Test
    void dataSourceInformation_returnsEdadataSourceInformation() {
        // Given
        String dsoId = "dsoId";
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.dsoId()).thenReturn(dsoId);
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);

        // Then
        assertEquals(EdaDataSourceInformation.class, permissionRequest.dataSourceInformation().getClass());
    }

    @Test
    void equalEdaPermissionRequests_returnTrue() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", "dataNeedId", ccmoRequest, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", "dataNeedId", ccmoRequest, factory);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertTrue(res);
    }

    @Test
    void differentObjectEdaPermissionRequestsEquals_returnsFalse() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, factory);

        // When
        boolean res = permissionRequest.equals(new Object());

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentConnectionId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("cid", "pid", ccmoRequest, factory);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentPermissionId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid1", ccmoRequest, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid2", ccmoRequest, factory);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentCMRequestId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest1 = mock(CCMORequest.class);
        when(ccmoRequest1.cmRequestId()).thenReturn("cmRequestId1");
        when(ccmoRequest1.messageId()).thenReturn("messageId");
        CCMORequest ccmoRequest2 = mock(CCMORequest.class);
        when(ccmoRequest2.cmRequestId()).thenReturn("cmRequestId2");
        when(ccmoRequest2.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest1, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest2, factory);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentConversationId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest1 = mock(CCMORequest.class);
        when(ccmoRequest1.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest1.messageId()).thenReturn("messageId1");
        CCMORequest ccmoRequest2 = mock(CCMORequest.class);
        when(ccmoRequest2.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest2.messageId()).thenReturn("messageId2");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest1, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest2, factory);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentStates_areNotEqual() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid1", ccmoRequest, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid2", ccmoRequest, factory);
        permissionRequest2.changeState(new AtInvalidPermissionRequestState(permissionRequest2));

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void equalEdaPermissionRequests_haveSameHash() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", "dataNeedId", ccmoRequest, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", "dataNeedId", ccmoRequest, factory);

        // When
        int res1 = permissionRequest1.hashCode();
        int res2 = permissionRequest2.hashCode();

        // Then
        assertEquals(res1, res2);
    }

    @Test
    void equalEdaPermissionRequests_haveDifferentHash() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, factory);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("cid", "pid2", ccmoRequest, factory);

        // When
        int res1 = permissionRequest1.hashCode();
        int res2 = permissionRequest2.hashCode();

        // Then
        assertNotEquals(res1, res2);
    }

    @Test
    void validatedTransitionsEdaPermissionRequest() throws StateTransitionException, InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(new CMRequest());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);

        // When
        permissionRequest.validate();

        // Then
        assertEquals(AtValidatedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void sendToPermissionAdministratorTransitionsEdaPermissionRequest() throws StateTransitionException, InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(new CMRequest());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        permissionRequest.validate();

        // When
        permissionRequest.sendToPermissionAdministrator();


        // Then
        assertEquals(AtPendingAcknowledgmentPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void receivedPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws StateTransitionException, InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(new CMRequest());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();

        // When
        permissionRequest.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(AtSentToPermissionAdministratorPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void acceptPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws StateTransitionException, InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(new CMRequest());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        permissionRequest.receivedPermissionAdministratorResponse();

        // When
        permissionRequest.accept();

        // Then
        assertEquals(AtAcceptedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalidPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws StateTransitionException, InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(new CMRequest());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        permissionRequest.receivedPermissionAdministratorResponse();

        // When
        permissionRequest.invalid();

        // Then
        assertEquals(AtInvalidPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void rejectPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws StateTransitionException, InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(new CMRequest());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        permissionRequest.receivedPermissionAdministratorResponse();

        // When
        permissionRequest.reject();

        // Then
        assertEquals(AtRejectedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void terminatePermissionRequest_terminates() throws StateTransitionException {
        // Given
        AtConfiguration atConfiguration = new PlainAtConfiguration("ep", null);
        TestPublisher<CMRequestStatus> testPublisher = TestPublisher.createCold();
        CMRequestStatus cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.DELIVERED, "", "convId");
        cmRequestStatus.setCmConsentId("consentId");
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(testPublisher.flux());
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        when(ccmoRequest.dsoId()).thenReturn("dsoId");
        var factory = new StateBuilderFactory(atConfiguration, edaAdapter);
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        permissionRequest.setConsentId("consentId");
        permissionRequest.changeState(new AtAcceptedPermissionRequestState(permissionRequest, edaAdapter, atConfiguration, factory));
        testPublisher.emit(cmRequestStatus);

        // When
        permissionRequest.terminate();

        // Then
        assertEquals(PermissionProcessStatus.TERMINATED, permissionRequest.state().status());
    }
}