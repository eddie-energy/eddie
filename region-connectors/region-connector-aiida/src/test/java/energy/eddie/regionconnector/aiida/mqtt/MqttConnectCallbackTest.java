// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.regionconnector.aiida.mqtt.callback.MqttConnectCallback;
import energy.eddie.regionconnector.aiida.mqtt.events.MqttConnectedEvent;
import energy.eddie.regionconnector.aiida.publisher.MqttEventPublisher;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttClientInterface;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttConnectCallbackTest {
    @Mock
    private MqttAsyncClient client;
    @Mock
    private MqttConnectionOptions connectionOptions;
    @Mock
    private ThreadPoolTaskScheduler scheduler;
    @Mock
    private IMqttToken mockToken;
    @Mock
    private MqttClientInterface mockClient;
    @Mock
    private MqttEventPublisher eventPublisher;

    @Test
    void onSuccess_publishesMqttConnectedEvent() {
        // Given
        var task = new MqttConnectCallback(client, connectionOptions, eventPublisher, scheduler);
        when(mockToken.getClient()).thenReturn(mockClient);
        when(mockClient.getServerURI()).thenReturn("fooBar");

        // When
        task.onSuccess(mockToken);

        // Then
        verify(eventPublisher).publishEvent(
                argThat(event -> ((MqttConnectedEvent) event).getServerUri().equals("fooBar")));
    }

    @Test
    void onFailure_schedulesTaskToReconnect() {
        // Given
        var task = new MqttConnectCallback(client, connectionOptions, eventPublisher, scheduler);
        when(mockToken.getClient()).thenReturn(mockClient);
        when(mockClient.getServerURI()).thenReturn("fooBar");

        // When
        task.onFailure(mockToken, new MqttException(999));

        // Then
        verify(scheduler).schedule(any(), any(Instant.class));
    }
}
