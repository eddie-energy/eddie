package energy.eddie.regionconnector.at.eda.permission.request;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.states.*;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


class StateBuilderFactoryTest {

    private static final AtConfiguration mockConfiguration = mock(AtConfiguration.class);
    private static final EdaAdapter mockEdaAdapter = mock(EdaAdapter.class);
    private static final AtPermissionRequest mockPermissionRequest = mock(AtPermissionRequest.class);
    private static final StateBuilderFactory factory = new StateBuilderFactory(mockConfiguration, mockEdaAdapter);

    private static Stream<Arguments> create_createsCorrectState() {
        return Stream.of(
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.CREATED).withCCMORequest(mock(CCMORequest.class)), AtCreatedPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.VALIDATED).withCMRequest(mock(CMRequest.class)), AtValidatedPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.MALFORMED).withCause(new Throwable()), AtMalformedPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT), AtPendingAcknowledgmentPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.UNABLE_TO_SEND).withCause(new Throwable()), AtUnableToSendPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR), AtSentToPermissionAdministratorPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.ACCEPTED), AtAcceptedPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.INVALID), AtInvalidPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.REJECTED), AtRejectedPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.TERMINATED), AtTerminatedPermissionRequestState.class),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.REVOKED), AtRevokedPermissionRequestState.class)
        );
    }

    private static Stream<Arguments> create_withoutRequiredArgs_throws() {
        return Stream.of(
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.CREATED)),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.VALIDATED)),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.MALFORMED)),
                Arguments.of(factory.create(mockPermissionRequest, PermissionProcessStatus.UNABLE_TO_SEND))
        );
    }

    @ParameterizedTest
    @MethodSource("create_createsCorrectState")
    void create_createsCorrectState(StateBuilderFactory.PermissionRequestStateBuilder builder, Class<? extends PermissionRequestState> expectedStateType) {
        // When
        PermissionRequestState state = builder.build();

        // Then
        assertInstanceOf(expectedStateType, state);
    }

    @ParameterizedTest
    @MethodSource("create_withoutRequiredArgs_throws")
    void create_withoutRequiredArgs_throws(StateBuilderFactory.PermissionRequestStateBuilder builder) {
        // Given, When, Then
        assertThrows(NullPointerException.class, builder::build);
    }
}