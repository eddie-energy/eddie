// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers.mqtt;

import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;

public record MqttStreamingContext(MqttAsyncClient client, MqttStreamingConfig streamingConfig,
                                   PermissionLatestRecordMap permissionLatestRecordMap) {
}
