// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.cim;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.cim.CimDataSource;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.health.contributor.Status;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CimAdapterTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(CimAdapter.class);
    private static final LogCaptor LOG_CAPTOR_ADAPTER = LogCaptor.forClass(DataSourceAdapter.class);
    private static final String CIM_TOPIC = "aiida/cim";
    private static final CimDataSource DATA_SOURCE = mock(CimDataSource.class);
    private static final String DATA_SOURCE_TOPIC = "aiida/test";
    private static final String DATA_SOURCE_INTERNAL_HOST = "tcp://localhost:1883";
    private static final MqttConfiguration MQTT_CONFIGURATION = mock(MqttConfiguration.class);
    private CimAdapter adapter;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        when(DATA_SOURCE.internalHost()).thenReturn(DATA_SOURCE_INTERNAL_HOST);
        when(DATA_SOURCE.topic()).thenReturn(DATA_SOURCE_TOPIC);
        when(DATA_SOURCE.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(MQTT_CONFIGURATION.password()).thenReturn("password");

        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        var mapper = builder.build();
        adapter = new CimAdapter(DATA_SOURCE, mapper, MQTT_CONFIGURATION);
        LOG_CAPTOR_ADAPTER.setLogLevelToDebug();
    }

    @AfterEach
    void tearDown() {
        LOG_CAPTOR.clearLogs();
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Test
    void testHealth() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(true);

            adapter.start();
            assertEquals(Status.UP, adapter.health().getStatus());

            adapter.close();
            when(mockClient.isConnected()).thenReturn(false);
            assertEquals(Status.DOWN, adapter.health().getStatus());
        }
    }

    @Test
    void verify_close_disconnectsAndClosesClient_andEmitsCompleteOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(true);

            StepVerifier.create(adapter.start()).then(adapter::close).expectComplete().verify();

            verify(mockClient).disconnect(anyLong());
            verify(mockClient).close();
        }
    }

    @Test
    void verify_whenClientDisconnected_close_doesNotCallDisconnect() throws MqttException {
        // calling .disconnect() on the MQTT client when it's not connected leads to an exception, therefore ensure to only call when it's connected

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(false);

            StepVerifier.create(adapter.start()).then(adapter::close).expectComplete().verify();

            verify(mockClient, never()).disconnect(anyLong());
            verify(mockClient).close();
        }
    }

    @Test
    void verify_errorsDuringClose_areLogged() throws MqttException {
        try (LogCaptor captor = LogCaptor.forClass(CimAdapter.class)) {
            try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
                var mockClient = mock(MqttAsyncClient.class);
                mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                               .thenReturn(mockClient);
                when(mockClient.disconnect(anyLong())).thenThrow(new MqttException(998877));
                when(mockClient.isConnected()).thenReturn(true);

                adapter.start().subscribe();
                adapter.close();

                assertThat(captor.getWarnLogs()).contains("Error while disconnecting or closing MQTT client");
            }
        }
    }

    @Test
    void verify_start_callsConnect() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            adapter.start().subscribe();

            verify(mockClient).connect(any());
        }
    }

    @Test
    void givenErrorDuringStart_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.connect(any())).thenThrow(new MqttException(998877));

            StepVerifier.create(adapter.start())
                        .expectErrorMatches(throwable -> ((MqttException) throwable).getReasonCode() == 998877)
                        .verify();
        }
    }

    @Test
    void givenPayloadFromMqttBroker_isPublishedOnFlux() {
        var payload = """
                {
                  "version": "1.0",
                  "registeredResource.mRID": {
                    "value": "0fecab2b-1c5e-4595-8d41-427850719410",
                    "codingScheme": "NAT"
                  },
                  "dateAndOrTime.dateTime": "2025-09-08T06:42:08Z",
                  "Quantity": [
                    {
                      "quantity": 1130,
                      "type": "0",
                      "quality": "AS_PROVIDED"
                    },
                    {
                      "quantity": 128,
                      "type": "2",
                      "quality": "AS_PROVIDED"
                    }
                  ]
                }
                """;

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived(CIM_TOPIC, message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                             .equals(POSITIVE_ACTIVE_ENERGY) && aiidaRecordValue.value()
                                                                                                                                                                .equals("1130")))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenConnectComplete_subscribesToTopic() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            adapter.start().subscribe();

            adapter.connectComplete(false, DATA_SOURCE_INTERNAL_HOST);

            verify(mockClient).subscribe(DATA_SOURCE_TOPIC, 2);
        }
    }

    @Test
    void givenErrorWhileSubscribing_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.subscribe(DATA_SOURCE_TOPIC, 2)).thenThrow(new MqttException(998877));

            StepVerifier.create(adapter.start())
                        .expectSubscription()
                        .then(() -> adapter.connectComplete(false, DATA_SOURCE_INTERNAL_HOST))
                        .expectError()
                        .verify();
        }
    }

    @Test
    void givenConnectionLost_warningIsLogged() {
        adapter.disconnected(new MqttDisconnectResponse(new MqttException(998877)));
        assertThat(LOG_CAPTOR.getWarnLogs()).contains("Disconnected from MQTT broker");
    }
}