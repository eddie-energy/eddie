// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the active MQTT client of each inbound data source so other inbound components can reuse its connection.
 */
@Component
public class InboundMqttClientRegistry {
    private final ConcurrentHashMap<UUID, IMqttAsyncClient> clients = new ConcurrentHashMap<>();

    public void register(UUID dataSourceId, IMqttAsyncClient mqttClient) {
        clients.put(dataSourceId, mqttClient);
    }

    public void unregister(UUID dataSourceId, IMqttAsyncClient mqttClient) {
        clients.remove(dataSourceId, mqttClient);
    }

    public Optional<IMqttAsyncClient> clientFor(UUID dataSourceId) {
        return Optional.ofNullable(clients.get(dataSourceId));
    }
}
