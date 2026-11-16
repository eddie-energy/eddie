// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound;

import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class InboundMqttClientRegistryTest {
    @Test
    void unregister_removesOnlyTheMatchingClient() {
        var registry = new InboundMqttClientRegistry();
        var dataSourceId = UUID.randomUUID();
        var registeredClient = mock(IMqttAsyncClient.class);

        registry.register(dataSourceId, registeredClient);
        registry.unregister(dataSourceId, mock(IMqttAsyncClient.class));

        assertEquals(registeredClient, registry.clientFor(dataSourceId).orElseThrow());

        registry.unregister(dataSourceId, registeredClient);
        assertTrue(registry.clientFor(dataSourceId).isEmpty());
    }
}
