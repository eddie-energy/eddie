package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TerminationAndFulfillmentHandlerTest {
    @Mock
    private OAuthTokenRepository oAuthTokenRepository;

    public static Stream<Arguments> testAccept_deletesOAuthTokenDetails() {
        return Stream.of(
                Arguments.of(new NlSimpleEvent("pid", PermissionProcessStatus.FULFILLED)),
                Arguments.of(new NlSimpleEvent("pid", PermissionProcessStatus.TERMINATED))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccept_deletesOAuthTokenDetails(PermissionEvent permissionEvent) {
        // Given
        var eventBus = new EventBusImpl();
        new TerminationAndFulfillmentHandler(eventBus, oAuthTokenRepository);

        // When
        eventBus.emit(permissionEvent);

        // Then
        verify(oAuthTokenRepository).deleteById("pid");

        // Clean-Up
        eventBus.close();
    }
}