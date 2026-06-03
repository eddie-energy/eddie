// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services.inbound;

import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.regionconnector.aiida.services.MqttService;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorPermissionCommandServiceTest {
    private static final UUID PERMISSION_ID = UUID.fromString("6211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final PermissionCommand COMMAND =
            new PermissionCommand.Terminate("aiida", PERMISSION_ID);

    @Mock
    private MqttService mqttService;
    @InjectMocks
    private AiidaRegionConnectorPermissionCommandService service;

    @Test
    void givenCommand_publishesViaMqttService() throws MqttException {
        // When
        service.permissionCommandArrived(COMMAND);

        // Then
        verify(mqttService).publishPermissionCommand(COMMAND);
    }

    @Test
    void givenMqttException_logsErrorAndDoesNotRethrow() throws MqttException {
        // Given
        doThrow(new MqttException(999)).when(mqttService).publishPermissionCommand(COMMAND);

        try (var logCaptor = LogCaptor.forClass(AiidaRegionConnectorPermissionCommandService.class)) {
            // When / Then
            assertThatCode(() -> service.permissionCommandArrived(COMMAND)).doesNotThrowAnyException();

            assertThat(logCaptor.getErrorLogs())
                    .anyMatch(log -> log.contains(PERMISSION_ID.toString()));
        }
    }
}