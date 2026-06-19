// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.MqttTestFixtures;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyPlugGen3DataSource;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.ObisCode;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ShellyPlugGen3AdapterTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(ShellyPlugGen3Adapter.class);
    private static final LogCaptor LOG_CAPTOR_ADAPTER = LogCaptor.forClass(DataSourceAdapter.class);
    private static final String TOPIC = "aiida/4211ea05-d4ab-48ff-8613-8f4791a56606/events/rpc";
    private static final ShellyPlugGen3DataSource DATA_SOURCE = mock(ShellyPlugGen3DataSource.class);
    private static final String DATA_SOURCE_INTERNAL_HOST = "tcp://localhost:1883";
    private static final String DATA_SOURCE_TOPIC = "aiida/#";
    private static final MqttConfiguration MQTT_CONFIGURATION = mock(MqttConfiguration.class);
    private ShellyPlugGen3Adapter adapter;

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
        adapter = new ShellyPlugGen3Adapter(DATA_SOURCE, mapper, MQTT_CONFIGURATION);
        LOG_CAPTOR_ADAPTER.setLogLevelToDebug();
    }

    @AfterEach
    void tearDown() {
        LOG_CAPTOR.clearLogs();
    }

    @Test
    void givenGen3SwitchPayload_isPublishedOnFlux() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(MqttTestFixtures.SHELLY_PLUG_GEN3_SWITCH_PAYLOAD.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived(TOPIC, message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue ->
                                                                                 aiidaRecordValue.dataTag()
                                                                                                 .equals(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                 aiidaRecordValue.value()
                                                                                                 .equals("0.035")
                                                               ))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenGen3SwitchPayload_allFieldsPresent_emitsAllMeasurements() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(MqttTestFixtures.SHELLY_PLUG_GEN3_SWITCH_PAYLOAD.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        .then(() -> adapter.messageArrived(TOPIC, message))
                        .expectNextMatches(received -> {
                            var values = received.aiidaRecordValues();
                            return values.stream().anyMatch(v -> v.dataTag().equals(ObisCode.INSTANTANEOUS_VOLTAGE)) &&
                                   values.stream().anyMatch(v -> v.dataTag().equals(ObisCode.INSTANTANEOUS_CURRENT)) &&
                                   values.stream().anyMatch(v -> v.dataTag().equals(ObisCode.FREQUENCY)) &&
                                   values.stream().anyMatch(v -> v.dataTag().equals(ObisCode.POSITIVE_ACTIVE_ENERGY));
                        })
                        .then(adapter::close)
                        .expectComplete()
                        .verify();
        }
    }

    @Test
    void givenInvalidJsonPayload_errorIsLogged() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage("{ invalid json }".getBytes(StandardCharsets.UTF_8));

            adapter.start().subscribe();
            adapter.messageArrived(TOPIC, message);

            assertThat(LOG_CAPTOR.getErrorLogs()).anyMatch(log -> log.contains("Error while deserializing"));
        }
    }
}