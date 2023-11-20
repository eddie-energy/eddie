package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SavingPermissionRequestTest {

    static Stream<String> savingPermissionRequestArguments() {
        return Stream.of(
                "validate",
                "sendToPermissionAdministrator",
                "receivedPermissionAdministratorResponse",
                "invalid",
                "accept",
                "rejected",
                "terminate"
        );
    }

    @ParameterizedTest
    @MethodSource("savingPermissionRequestArguments")
    void savingPermissionRequest_invokingMethodUpdatesSave(String actionMethodName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        Method actionMethod = SavingPermissionRequest.class.getMethod(actionMethodName);
        actionMethod.invoke(savingPermissionRequest);

        // Then
        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_returnsStateOfWrappedPermissionRequest() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        var state = savingPermissionRequest.state();

        // Then
        assertEquals(createdState, state);
    }

    @Test
    void savingPermissionRequest_changesStateOfWrappedPermissionRequest() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, savingPermissionRequest.state());
    }

    @Test
    void savingPermissionRequest_returnsPermissionId() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        String permissionId = savingPermissionRequest.permissionId();

        // Then
        assertEquals("permissionId", permissionId);
    }

    @Test
    void savingPermissionRequest_returnsConnectionId() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        String connectionId = savingPermissionRequest.connectionId();

        // Then
        assertEquals("connectionId", connectionId);
    }

    @Test
    void savingPermissionRequest_returnsDataNeedId() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        String dataNeedId = savingPermissionRequest.dataNeedId();

        // Then
        assertEquals("dataNeedId", dataNeedId);
    }


    @Test
    void savingPermissionRequest_equalsReturnsTrueForDecoratee() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        PermissionRequest savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        var res = savingPermissionRequest.equals(permissionRequest);

        // Then
        assertTrue(res);
    }

    @Test
    void savingPermissionRequest_equalsReturnsFalse() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest1 = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest1, repo);

        // When
        var res = savingPermissionRequest.equals(new Object());

        // Then
        assertFalse(res);
    }

    @Test
    void savingPermissionRequest_hashCodeIsSameForDecoratee() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        var res = savingPermissionRequest.hashCode();

        // Then
        assertEquals(permissionRequest.hashCode(), res);
    }

    private final static class SimplePermissionRequestRepository implements PermissionRequestRepository<PermissionRequest> {
        private PermissionRequest permissionRequest = null;

        @Override
        public void save(PermissionRequest request) {
            permissionRequest = request;
        }

        @Override
        public Optional<PermissionRequest> findByPermissionId(String permissionId) {
            return Optional.ofNullable(permissionRequest)
                    .filter(r -> r.permissionId().equals(permissionId));
        }

        @Override
        public boolean removeByPermissionId(String permissionId) {
            if (findByPermissionId(permissionId).isPresent()) {
                permissionRequest = null;
                return true;
            }
            return false;
        }
    }
}