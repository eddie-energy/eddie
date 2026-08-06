// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.provisioning;

import energy.eddie.aiida.models.mqtt.MqttConnection;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.utils.MqttFactory;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProvisioningMqttPublisherTest {
    private static final String INTERNAL_HOST = "tcp://localhost:1883";
    private static final String USERNAME = "provisioning-user";
    private static final String PASSWORD = "provisioning-password";
    private static final String PASSWORD_HASH = "provisioning-password-hash";
    private static final String AIIDA_USERNAME = "aiida";
    private static final String AIIDA_PASSWORD = "aiida-password";
    private static final String TOPIC = "aiida/inbound/test";

    private MqttConnection mqttConnection;
    private MqttAsyncClient client;

    @BeforeEach
    void setUp() {
        mqttConnection = new MqttConnection(INTERNAL_HOST, INTERNAL_HOST);
        mqttConnection.createMqttUser(USERNAME, PASSWORD);
        client = mock(MqttAsyncClient.class);
    }

    @Test
    void constructor_connectsWithProvisioningCredentials() throws MqttException {
        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturningClient();
             var publisher = new ProvisioningMqttPublisher(mqttConnection, TOPIC)) {
            var optionsCaptor = ArgumentCaptor.forClass(MqttConnectionOptions.class);
            verify(client).setCallback(publisher);
            verify(client).connect(optionsCaptor.capture());
            assertThat(optionsCaptor.getValue().getUserName()).isEqualTo(USERNAME);
            assertThat(new String(optionsCaptor.getValue().getPassword(), StandardCharsets.UTF_8)).isEqualTo(PASSWORD);
        }
    }

    @Test
    void constructor_withAiidaCredentials_doesNotAuthenticateAsPersistedProvisioningUser() throws MqttException {
        mqttConnection.createMqttUser(USERNAME, PASSWORD_HASH);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturningClient();
             var publisher = new ProvisioningMqttPublisher(
                     mqttConnection,
                     TOPIC,
                     AIIDA_USERNAME,
                     AIIDA_PASSWORD
             )) {
            var optionsCaptor = ArgumentCaptor.forClass(MqttConnectionOptions.class);
            verify(client).setCallback(publisher);
            verify(client).connect(optionsCaptor.capture());
            assertThat(optionsCaptor.getValue().getUserName()).isEqualTo(AIIDA_USERNAME);
            assertThat(new String(optionsCaptor.getValue().getPassword(), StandardCharsets.UTF_8))
                    .isEqualTo(AIIDA_PASSWORD);
        }
    }

    @Test
    void publish_whenConnected_publishesPayloadToTopic() throws MqttException {
        var inboundRecord = mock(InboundRecord.class);
        when(inboundRecord.payload()).thenReturn("payload");
        when(client.isConnected()).thenReturn(true);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturningClient();
             var publisher = new ProvisioningMqttPublisher(mqttConnection, TOPIC)) {
            publisher.publish(inboundRecord);

            verify(client).publish(
                    eq(TOPIC),
                    argThat(message -> new String(message.getPayload(), StandardCharsets.UTF_8).equals("payload"))
            );
        }
    }

    @Test
    void publish_whenDisconnected_doesNotPublish() throws MqttException {
        var inboundRecord = mock(InboundRecord.class);
        when(inboundRecord.payload()).thenReturn("payload");
        when(client.isConnected()).thenReturn(false);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturningClient();
             var publisher = new ProvisioningMqttPublisher(mqttConnection, TOPIC)) {
            publisher.publish(inboundRecord);

            verify(client, never()).publish(anyString(), any(MqttMessage.class));
        }
    }

    @Test
    void close_whenConnected_disconnectsAndClosesClient() throws MqttException {
        when(client.isConnected()).thenReturn(true);

        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturningClient()) {
            try (var ignored1 = new ProvisioningMqttPublisher(mqttConnection, TOPIC)) {
                // Closing is exercised by try-with-resources.
            }

            verify(client).disconnect(anyLong());
            verify(client).close();
        }
    }

    @Test
    void connectComplete_doesNotSubscribeToProvisioningTopic() throws MqttException {
        try (MockedStatic<MqttFactory> ignored = mqttFactoryReturningClient();
             var publisher = new ProvisioningMqttPublisher(mqttConnection, TOPIC)) {
            publisher.connectComplete(false, INTERNAL_HOST);

            verify(client, never()).subscribe(anyString(), anyInt());
        }
    }

    private MockedStatic<MqttFactory> mqttFactoryReturningClient() {
        var mockMqttFactory = mockStatic(MqttFactory.class);
        mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                       .thenReturn(client);
        return mockMqttFactory;
    }
}
