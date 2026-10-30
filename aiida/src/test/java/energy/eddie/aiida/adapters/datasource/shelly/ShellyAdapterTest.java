// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.shelly.transformer.ShellyJsonTest;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.shelly.ShellyDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.ObisCode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ShellyAdapterTest {
    private static final String PLUG_M_GEN3_PAYLOAD = """
            {
              "src": "shellyplugmg3-907069447380",
              "dst": "aiida/test/events",
              "method": "NotifyStatus",
              "params": {
                "ts": 1792497540.00,
                "switch:0": {
                  "aenergy": {
                    "by_minute": [206.766, 413.532, 413.532],
                    "minute_ts": 1792497540,
                    "total": 204.484
                  },
                  "apower": 18.3,
                  "current": 0.158,
                  "freq": 50.03,
                  "ret_aenergy": {
                    "by_minute": [0.0, 0.0, 0.0],
                    "minute_ts": 1792497540,
                    "total": 0.0
                  },
                  "voltage": 231.5
                }
              }
            }
            """;
    private static final String PM1_PAYLOAD = """
            {
              "src": "shellypluspmmini-44179bf90f08",
              "dst": "aiida/test/events",
              "method": "NotifyFullStatus",
              "params": {
                "ts": 1792497540.0,
                "pm1:0": {
                  "id": 0,
                  "voltage": 225.9,
                  "current": 0.5,
                  "apower": 100.0,
                  "aprtpower": 112.0,
                  "pf": 0.9,
                  "freq": 50.0,
                  "aenergy": {"total": 11.679, "minute_ts": 1792497540},
                  "ret_aenergy": {"total": 4.126, "minute_ts": 1792497540}
                }
              }
            }
            """;
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(ShellyAdapter.class);
    private static final LogCaptor LOG_CAPTOR_ADAPTER = LogCaptor.forClass(DataSourceAdapter.class);
    private static final String TOPIC = "aiida/4211ea05-d4ab-48ff-8613-8f4791a56606/events/rpc";
    private static final ShellyDataSource DATA_SOURCE = mock(ShellyDataSource.class);
    private static final String DATA_SOURCE_INTERNAL_HOST = "tcp://localhost:1883";
    private static final String DATA_SOURCE_TOPIC = "aiida/#";
    private static final MqttConfiguration MQTT_CONFIGURATION = mock(MqttConfiguration.class);
    private ShellyAdapter adapter;

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
        adapter = new ShellyAdapter(DATA_SOURCE, mapper, MQTT_CONFIGURATION);
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
            assertEquals(Status.UNKNOWN, adapter.health().getStatus());

            adapter.messageArrived(TOPIC, new MqttMessage("true".getBytes(StandardCharsets.UTF_8)));
            assertEquals(Status.UP, adapter.health().getStatus());

            adapter.messageArrived(TOPIC, new MqttMessage("false".getBytes(StandardCharsets.UTF_8)));
            assertEquals(Status.DOWN, adapter.health().getStatus());

            adapter.messageArrived(TOPIC, new MqttMessage("true".getBytes(StandardCharsets.UTF_8)));
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
        try (LogCaptor captor = LogCaptor.forClass(ShellyAdapter.class)) {
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
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(ShellyJsonTest.EM_PAYLOAD.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.startFiltered(AiidaRecord.class))
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived(TOPIC, message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue ->
                                                                                 aiidaRecordValue.dataTag()
                                                                                                 .equals(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                 aiidaRecordValue.value()
                                                                                                 .equals("-0.018017")
                                                               ))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenPlugMGen3Payload_publishesOnlyDocumentedElectricalMeasurements() {
        StepVerifier.create(adapter.startFiltered(AiidaRecord.class))
                    .then(() -> adapter.messageArrived(TOPIC, mqttMessage(PLUG_M_GEN3_PAYLOAD)))
                    .assertNext(aiidaRecord -> {
                        assertThat(aiidaRecord.aiidaRecordValues()).hasSize(6);
                        assertThat(aiidaRecord.aiidaRecordValues())
                                .extracting(AiidaRecordValue::rawTag)
                                .containsExactlyInAnyOrder(
                                        "switch:0:aenergy.total",
                                        "switch:0:apower",
                                        "switch:0:current",
                                        "switch:0:freq",
                                        "switch:0:ret_aenergy.total",
                                        "switch:0:voltage"
                                );
                        assertThat(aiidaRecord.aiidaRecordValues())
                                .filteredOn(value -> value.rawTag().equals("switch:0:ret_aenergy.total"))
                                .extracting(AiidaRecordValue::dataTag)
                                .containsExactly(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1);
                    })
                    .then(adapter::close)
                    .verifyComplete();
    }

    @Test
    void givenPM1FullStatus_publishesEquivalentMeteringMeasurements() {
        StepVerifier.create(adapter.startFiltered(AiidaRecord.class))
                    .then(() -> adapter.messageArrived(TOPIC, mqttMessage(PM1_PAYLOAD)))
                    .assertNext(aiidaRecord -> {
                        assertThat(aiidaRecord.aiidaRecordValues()).hasSize(8);
                        assertThat(aiidaRecord.aiidaRecordValues())
                                .filteredOn(value -> value.rawTag().equals("pm1:0:aprtpower"))
                                .extracting(AiidaRecordValue::dataTag, AiidaRecordValue::value)
                                .containsExactly(tuple(ObisCode.APPARENT_INSTANTANEOUS_POWER, "0.112"));
                    })
                    .then(adapter::close)
                    .verifyComplete();
    }

    @Test
    void givenNotifyEvent_doesNotPublishMeasurementRecord() {
        var eventPayload = """
                {"src":"shelly","dst":"aiida","method":"NotifyEvent","params":{"ts":1,"events":[]}}
                """;

        StepVerifier.create(adapter.startFiltered(AiidaRecord.class))
                    .then(() -> adapter.messageArrived(TOPIC, mqttMessage(eventPayload)))
                    .then(adapter::close)
                    .verifyComplete();
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

    private static MqttMessage mqttMessage(String payload) {
        return new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
    }
}
