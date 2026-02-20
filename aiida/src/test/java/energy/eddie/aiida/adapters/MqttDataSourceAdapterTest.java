// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Status;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MqttDataSourceAdapterTest {

    private static final MqttConfiguration MQTT_CONFIGURATION = mock(MqttConfiguration.class);
    private static final MqttDataSource DATA_SOURCE = mock(MqttDataSource.class);
    private static final String DATA_SOURCE_INTERNAL_HOST = "tcp://localhost:1883";
    private static final String DATA_SOURCE_TOPIC = "aiida/test";

    @BeforeEach
    void setup() {
        when(DATA_SOURCE.internalHost()).thenReturn(DATA_SOURCE_INTERNAL_HOST);
        when(DATA_SOURCE.topic()).thenReturn(DATA_SOURCE_TOPIC);
        when(DATA_SOURCE.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(MQTT_CONFIGURATION.password()).thenReturn("password");
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Test
    void testHealth() {
        var adapter = new HealthTestMqttDataSourceAdapter(DATA_SOURCE, MQTT_CONFIGURATION);

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(true);

            adapter.start();
            assertEquals(Status.UNKNOWN, Objects.requireNonNull(adapter.health()).getStatus()); // No messages found yet

            adapter.close();
            when(mockClient.isConnected()).thenReturn(false);
            assertEquals(Status.DOWN, Objects.requireNonNull(adapter.health()).getStatus());
        }
    }

    private static class HealthTestMqttDataSourceAdapter extends MqttDataSourceAdapter<MqttDataSource> {

        private static final Logger LOGGER = LoggerFactory.getLogger(HealthTestMqttDataSourceAdapter.class);

        protected HealthTestMqttDataSourceAdapter(MqttDataSource dataSource, MqttConfiguration mqttConfiguration) {
            super(dataSource, LOGGER, mqttConfiguration);
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) {
            // Implementation not needed for test setup
        }
    }
}


