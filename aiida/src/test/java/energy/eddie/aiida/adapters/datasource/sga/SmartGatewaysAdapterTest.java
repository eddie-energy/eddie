// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysTopic;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SmartGatewaysAdapterTest {
    private static final String TOPIC_PREFIX = "aiida/abcd";
    private static final SmartGatewaysDataSource DATA_SOURCE = mock(SmartGatewaysDataSource.class);
    private static final MqttConfiguration MQTT_CONFIGURATION = mock(MqttConfiguration.class);

    private SmartGatewaysAdapter adapter;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        when(DATA_SOURCE.internalHost()).thenReturn("tcp://localhost:1883");
        when(DATA_SOURCE.topic()).thenReturn(TOPIC_PREFIX + "/dsmr/reading/+");
        when(DATA_SOURCE.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(MQTT_CONFIGURATION.password()).thenReturn("password");

        adapter = new SmartGatewaysAdapter(DATA_SOURCE, MQTT_CONFIGURATION);
    }

    @AfterEach
    void tearDown() {
        adapter.close();
        LogCaptor.forClass(SmartGatewaysAdapter.class).clearLogs();
    }

    @Test
    void verify_close_disconnectsAndClosesClient_andEmitsCompleteOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(true);

            StepVerifier.create(adapter.start()).then(adapter::close).expectComplete().verify();

            verify(mockClient).disconnect(anyLong());
            verify(mockClient).close();
        }
    }

    @Test
    void verify_whenClientDisconnected_close_doesNotCallDisconnect() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(false);

            StepVerifier.create(adapter.start()).then(adapter::close).expectComplete().verify();

            verify(mockClient, never()).disconnect(anyLong());
            verify(mockClient).close();
        }
    }

    @Test
    void verify_errorsDuringClose_areLogged() throws MqttException {
        try (LogCaptor logCaptor = LogCaptor.forClass(SmartGatewaysAdapter.class)) {
            try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
                var mockClient = mock(MqttAsyncClient.class);
                mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);
                when(mockClient.disconnect(anyLong())).thenThrow(new MqttException(999));
                when(mockClient.isConnected()).thenReturn(true);

                adapter.start().subscribe();
                adapter.close();

                assertThat(logCaptor.getWarnLogs()).anyMatch(log -> log.contains("Error while disconnecting or closing MQTT client"));
            }
        }
    }

    @Test
    void verify_start_callsConnect() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);

            adapter.start().subscribe();

            verify(mockClient).connect(any());
        }
    }

    @Test
    void givenErrorDuringStart_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);
            when(mockClient.connect(any())).thenThrow(new MqttException(998877));

            StepVerifier.create(adapter.start())
                        .expectErrorMatches(t -> ((MqttException) t).getReasonCode() == 998877)
                        .verify();
        }
    }

    @Test
    void givenBatchCompleted_emitsAiidaRecord() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);

            StepVerifier.create(adapter.startFiltered(AiidaRecord.class)).then(() -> {
                            for (SmartGatewaysTopic t : SmartGatewaysTopic.values()) {
                                if (t.isExpected()) {
                                    send(t.topic(), "45");
                                }
                            }
                        })
                        .expectNextMatches(aiidaRecord ->
                                                   aiidaRecord.aiidaRecordValues()
                                                              .stream()
                                                              .anyMatch(v ->
                                                                                v.dataTag() == POSITIVE_ACTIVE_ENERGY
                                                                                && v.value().equals("90"))
                                                   && aiidaRecord.aiidaRecordValues()
                                                                 .stream()
                                                                 .anyMatch(v ->
                                                                                   v.dataTag() == POSITIVE_ACTIVE_INSTANTANEOUS_POWER
                                                                                   && v.value().equals("45"))
                        )
                        .then(adapter::close)
                        .expectComplete()
                        .verify();
        }
    }

    @Test
    void givenDifferentDevicePrefixes_onlyConfiguredDeviceEmitsRecord() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);

            StepVerifier.create(adapter.startFiltered(AiidaRecord.class))
                        .expectSubscription()
                        .then(() -> {
                            for (SmartGatewaysTopic topic : SmartGatewaysTopic.values()) {
                                if (topic.isExpected()) {
                                    var message = new MqttMessage("999".getBytes(StandardCharsets.UTF_8));
                                    adapter.messageArrived("aiida/other/" + topic.topic(), message);
                                    adapter.messageArrived("aiida/" + topic.topic(), message);
                                }
                            }
                        })
                        .expectNoEvent(Duration.ofMillis(50))
                        .then(() -> {
                            for (SmartGatewaysTopic topic : SmartGatewaysTopic.values()) {
                                if (topic.isExpected()) {
                                    send(topic.topic(), "1");
                                }
                            }
                        })
                        .expectNextMatches(record -> record.aiidaRecordValues().stream()
                                                           .anyMatch(value -> value.dataTag() == POSITIVE_ACTIVE_ENERGY
                                                                              && value.value().equals("2")))
                        .then(adapter::close)
                        .expectComplete()
                        .verify();
        }
    }

    @Test
    void givenIncompleteBatch_timeoutEmitsAvailablePowerWithoutInventingEnergyTotal() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any())).thenReturn(mockClient);

            StepVerifier.create(adapter.startFiltered(AiidaRecord.class))
                        .then(() -> {
                            send(SmartGatewaysTopic.ELECTRICITY_DELIVERED_1.topic(), "100.125");
                            send(SmartGatewaysTopic.ELECTRICITY_CURRENTLY_DELIVERED.topic(), "0.531");
                        })
                        .expectNextMatches(record -> record.aiidaRecordValues().size() == 1
                                                     && record.aiidaRecordValues().getFirst().dataTag()
                                                        == POSITIVE_ACTIVE_INSTANTANEOUS_POWER
                                                     && record.aiidaRecordValues().getFirst().value().equals("0.531"))
                        .then(adapter::close)
                        .expectComplete()
                        .verify(Duration.ofSeconds(20));
        }
    }

    @Test
    void invalidSubscriptionIsRejected() {
        for (String topic : List.of("/dsmr/reading/+", "aiida/abcd/#")) {
            when(DATA_SOURCE.topic()).thenReturn(topic);
            assertThatThrownBy(() -> new SmartGatewaysAdapter(DATA_SOURCE, MQTT_CONFIGURATION))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private void send(String topicSuffix, String value) {
        String topic = TOPIC_PREFIX + "/" + topicSuffix;
        MqttMessage message = new MqttMessage(value.getBytes(StandardCharsets.UTF_8));
        adapter.messageArrived(topic, message);
    }
}
