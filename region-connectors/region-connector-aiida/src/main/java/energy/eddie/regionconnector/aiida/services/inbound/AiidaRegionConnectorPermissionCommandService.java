// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services.inbound;

import energy.eddie.api.agnostic.command.RegionConnectorPermissionCommandService;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.regionconnector.aiida.services.MqttService;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiidaRegionConnectorPermissionCommandService implements RegionConnectorPermissionCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaRegionConnectorPermissionCommandService.class);

    private final MqttService mqttService;

    public AiidaRegionConnectorPermissionCommandService(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @Override
    public void permissionCommandArrived(PermissionCommand permissionCommand) {
        var permissionId = permissionCommand.permissionId();

        LOGGER.info("Received permission command with permissionId '{}'", permissionId);

        try {
            mqttService.publishPermissionCommand(permissionCommand);
        } catch (MqttException e) {
            LOGGER.error("Failed to publish permission command with permissionId '{}'", permissionId, e);
        }
    }
}
