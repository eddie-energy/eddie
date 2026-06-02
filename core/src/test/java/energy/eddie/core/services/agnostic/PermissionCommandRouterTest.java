// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.agnostic;

import energy.eddie.api.agnostic.command.RegionConnectorPermissionCommandService;
import energy.eddie.api.agnostic.outbound.PermissionCommandOutboundConnector;
import energy.eddie.cim.agnostic.PermissionCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class PermissionCommandRouterTest {
    private static final String REGION_CONNECTOR_1 = "rc-1";
    private static final String REGION_CONNECTOR_2 = "rc-2";

    @Mock
    private RegionConnectorPermissionCommandService permissionCommandService1;
    @Mock
    private RegionConnectorPermissionCommandService permissionCommandService2;

    @Test
    void registerPermissionCommandConnector_routesCommandsToRegisteredServices() {
        // Given
        var publisher = TestPublisher.<PermissionCommand>create();
        var router = new PermissionCommandRouter();
        router.registerPermissionCommandService(REGION_CONNECTOR_1, permissionCommandService1);
        router.registerPermissionCommandService(REGION_CONNECTOR_2, permissionCommandService2);
        router.registerPermissionCommandConnector(new PlainPermissionCommandOutboundConnector(publisher.flux()));

        var command1 = commandFor(REGION_CONNECTOR_1);
        var command2 = commandFor(REGION_CONNECTOR_2);
        var commandUnknown = commandFor("unknown-rc");

        // When
        publisher.emit(command1, command2, commandUnknown);

        // Then
        verify(permissionCommandService1, times(1)).permissionCommandArrived(command1);
        verify(permissionCommandService2, times(1)).permissionCommandArrived(command2);
    }

    @Test
    void registerPermissionCommandService_withSameId_overridesPreviousService() {
        // Given
        var publisher = TestPublisher.<PermissionCommand>create();
        var router = new PermissionCommandRouter();
        router.registerPermissionCommandService(REGION_CONNECTOR_1, permissionCommandService1);
        router.registerPermissionCommandService(REGION_CONNECTOR_1, permissionCommandService2);
        router.registerPermissionCommandConnector(new PlainPermissionCommandOutboundConnector(publisher.flux()));
        var command = commandFor(REGION_CONNECTOR_1);

        // When
        publisher.emit(command);

        // Then
        verify(permissionCommandService1, never()).permissionCommandArrived(any());
        verify(permissionCommandService2, times(1)).permissionCommandArrived(command);
    }

    @Test
    void close_disposesSubscriptions() throws Exception {
        // Given
        var publisher = TestPublisher.<PermissionCommand>create();
        var router = new PermissionCommandRouter();
        router.registerPermissionCommandService(REGION_CONNECTOR_1, permissionCommandService1);
        router.registerPermissionCommandConnector(new PlainPermissionCommandOutboundConnector(publisher.flux()));
        var command1 = commandFor(REGION_CONNECTOR_1);
        var command2 = commandFor(REGION_CONNECTOR_1);

        // When
        publisher.emit(command1);
        router.close();
        publisher.emit(command2);

        // Then
        verify(permissionCommandService1, times(1)).permissionCommandArrived(command1);
        verify(permissionCommandService1, never()).permissionCommandArrived(command2);
    }

    private static PermissionCommand commandFor(String regionConnectorId) {
        return new PermissionCommand.SetTransmissionEnabled(regionConnectorId,
                                                            UUID.randomUUID(),
                                                            ZonedDateTime.now(),
                                                            true);
    }

    private record PlainPermissionCommandOutboundConnector(
            Flux<PermissionCommand> flux
    ) implements PermissionCommandOutboundConnector {

        @Override
        public Flux<PermissionCommand> getPermissionCommands() {
            return flux;
        }
    }
}