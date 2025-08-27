package energy.eddie.aiida.adapters.datasource.at;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.at.OesterreichsEnergieDataSource;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.aiida.utils.TestUtils;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static energy.eddie.aiida.utils.ObisCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OesterreichsEnergieAdapterTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(OesterreichsEnergieAdapter.class);
    private static final LogCaptor LOG_CAPTOR_ADAPTER = LogCaptor.forClass(DataSourceAdapter.class);
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final OesterreichsEnergieDataSource DATA_SOURCE = new OesterreichsEnergieDataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.SMART_METER_ADAPTER,
                              AiidaAsset.SUBMETER,
                              "sma",
                              "AT",
                              true,
                              null,
                              null,
                              null),
            USER_ID,
            new DataSourceMqttDto("tcp://localhost:1883",
                                  "tcp://localhost:1883",
                                  "aiida/test",
                                  "user",
                                  "password")
    );
    private OesterreichsEnergieAdapter adapter;
    private ObjectMapper mapper;
    private MqttConfiguration mqttConfiguration;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        mapper = new AiidaConfiguration().customObjectMapper().build();
        mqttConfiguration = new MqttConfiguration(
                "tcp://localhost:1883",
                "tcp://localhost:1883",
                10,
                "password",
                ""
        );
        adapter = new OesterreichsEnergieAdapter(DATA_SOURCE, mapper, mqttConfiguration);

        LOG_CAPTOR_ADAPTER.setLogLevelToTrace();
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
        try (LogCaptor captor = LogCaptor.forClass(OesterreichsEnergieAdapter.class)) {
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
    void givenRecordFromMqttBrokerWithoutTimestamp_isPublishedOnFlux() {
        var recordJson = "{\"0-0:96.1.0\":{\"value\":\"90296857\"},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2370.6}";
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(recordJson.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("MyTestTopic", message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                             .equals(DEVICE_ID_1) && aiidaRecordValue.value()
                                                                                                                                                     .equals("90296857")))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenRecordFromMqttBroker_isPublishedOnFlux() {
        var recordJson = "{\"1-0:1.7.0\":{\"value\":152,\"time\":1698915600},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":782238.7}";

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(recordJson.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("MyTestTopic", message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                             .equals(POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                             aiidaRecordValue.value()
                                                                                                             .equals(String.valueOf(
                                                                                                                     152 / 1000f))
                                                               ))
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

            adapter.connectComplete(false, DATA_SOURCE.mqttInternalHost());

            verify(mockClient).subscribe(DATA_SOURCE.mqttSubscribeTopic(), 2);
        }
    }

    @Test
    void givenErrorWhileSubscribing_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.subscribe(DATA_SOURCE.mqttSubscribeTopic(), 2)).thenThrow(new MqttException(998877));

            StepVerifier.create(adapter.start())
                        .expectSubscription()
                        .then(() -> adapter.connectComplete(false, DATA_SOURCE.mqttInternalHost()))
                        .expectError()
                        .verify();
        }
    }

    @Test
    void givenInvalidJsonMessage_errorIsLogged() {
        var invalidJson = "{\"foo\":\"bar\"}";
        var validJson = "{\"1-0:2.7.0\":{\"value\":0,\"time\":1697622950},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2370.6}";

        StepVerifier stepVerifier = StepVerifier.create(adapter.start())
                                                .expectNextMatches(received -> received.aiidaRecordValues()
                                                                                       .stream()
                                                                                       .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                                                     .equals(NEGATIVE_ACTIVE_INSTANTANEOUS_POWER)))
                                                .then(adapter::close)
                                                .expectComplete()
                                                .verifyLater();

        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(invalidJson.getBytes(StandardCharsets.UTF_8)));
        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(validJson.getBytes(StandardCharsets.UTF_8)));

        TestUtils.verifyErrorLogStartsWith("Error while deserializing JSON received from adapter. JSON was %s".formatted(
                invalidJson), LOG_CAPTOR, JsonMappingException.class);

        stepVerifier.verify();
    }

    /**
     * When receiving a JSON with an unmapped/unknown OBIS code, for the known OBIS codes,
     * {@link energy.eddie.aiida.models.record.AiidaRecord}s are still emitted and the parsing does not fail
     * when encountering the unknown OBIS code.
     */
    @Test
    void givenUnknownObisCode_otherValuesAreStillEmitted() {
        var json = "{\"1-0:1.8.0\":{\"value\":83622,\"time\":1698218800},\"UNKNOWN-OBIS-CODE\":{\"value\":0,\"time\":0},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":83854.3}";

        StepVerifier.create(adapter.start())
                    .then(() -> adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                                                       new MqttMessage(json.getBytes(StandardCharsets.UTF_8))))
                    .expectNextMatches(received -> received.aiidaRecordValues()
                                                           .stream()
                                                           .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                         .equals(POSITIVE_ACTIVE_ENERGY)))
                    .then(adapter::close)
                    .expectComplete()
                    .verify();

        assertThat(LOG_CAPTOR_ADAPTER.getTraceLogs()).contains("Found unknown OBIS-CODES from " + DATA_SOURCE.asset() + ": [UNKNOWN-OBIS-CODE]");
    }

    @Test
    void givenConnectionLost_warningIsLogged() {
        adapter.disconnected(new MqttDisconnectResponse(new MqttException(998877)));
        assertThat(LOG_CAPTOR.getWarnLogs()).contains("Disconnected from MQTT broker");
    }
}