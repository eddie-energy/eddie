package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.aiida.utils.TestUtils;
import energy.eddie.dataneeds.validation.aiida.asset.AiidaAsset;
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

import static energy.eddie.aiida.utils.MqttConfig.MqttConfigBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OesterreichsEnergieAdapterTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(OesterreichsEnergieAdapter.class);
    private static final LogCaptor logCaptorAiidaDataSource = LogCaptor.forClass(AiidaDataSource.class);
    private OesterreichsEnergieAdapter adapter;
    private MqttConfig config;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        config = new MqttConfigBuilder("tcp://localhost:1883", "MyTestTopic").build();
        mapper = new AiidaConfiguration().objectMapper();
        adapter = new OesterreichsEnergieAdapter("1", config, mapper);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
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

            StepVerifier.create(adapter.start())
                        .then(adapter::close)
                        .expectComplete()
                        .verify();

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

            StepVerifier.create(adapter.start())
                        .then(adapter::close)
                        .expectComplete()
                        .verify();

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
    void givenUsernameAndPassword_isUsedByAdapter() {
        config = new MqttConfigBuilder("tcp://localhost:1883", "MyTestTopic")
                .setUsername("User")
                .setPassword("Pass")
                .build();
        config = spy(config);
        adapter = new OesterreichsEnergieAdapter("1", config, mapper);

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            adapter.start().subscribe();

            verify(config, atLeastOnce()).username();
            verify(config, atLeastOnce()).password();
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
                        .expectNextMatches(received -> received.aiidaRecordValue().stream()
                                                               .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                             .equals("0-0:96.1.0")
                                                                                             && aiidaRecordValue.value()
                                                                                                                .equals("90296857")))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenRecordFromMqttBroker_isPublishedOnFlux() {
        var recordJson = "{\"1-0:1.7.0\":{\"value\":45,\"time\":1698915600},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":782238.7}";

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(recordJson.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("MyTestTopic", message))
                        .expectNextMatches(received -> received.aiidaRecordValue().stream()
                                                               .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                             .equals("1-0:1.7.0")
                                                                                             && aiidaRecordValue.value()
                                                                                                                .equals("45")))
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

            adapter.connectComplete(false, config.serverURI());

            verify(mockClient).subscribe(config.subscribeTopic(), 2);
        }
    }

    @Test
    void givenErrorWhileSubscribing_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.subscribe(config.subscribeTopic(), 2)).thenThrow(new MqttException(998877));

            StepVerifier.create(adapter.start())
                        .expectSubscription()
                        .then(() -> adapter.connectComplete(false, config.serverURI()))
                        .expectError()
                        .verify();
        }
    }

    @Test
    void givenInvalidJsonMessage_errorIsLogged() {
        var invalidJson = "{\"foo\":\"bar\"}";
        var validJson = "{\"1-0:2.7.0\":{\"value\":0,\"time\":1697622950},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2370.6}";

        StepVerifier stepVerifier = StepVerifier.create(adapter.start())
                                                .expectNextMatches(received -> received.aiidaRecordValue().stream()
                                                                                       .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                                                     .equals("1-0:2.7.0")))
                                                .then(adapter::close)
                                                .expectComplete()
                                                .verifyLater();

        adapter.messageArrived(config.subscribeTopic(), new MqttMessage(invalidJson.getBytes(StandardCharsets.UTF_8)));
        adapter.messageArrived(config.subscribeTopic(), new MqttMessage(validJson.getBytes(StandardCharsets.UTF_8)));

        TestUtils.verifyErrorLogStartsWith("Error while deserializing JSON received from adapter. JSON was %s".formatted(
                                                   invalidJson),
                                           logCaptor, JsonMappingException.class);

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
                    .then(() -> adapter.messageArrived(config.subscribeTopic(),
                                                       new MqttMessage(json.getBytes(StandardCharsets.UTF_8))))
                    .expectNextMatches(received -> received.aiidaRecordValue().stream()
                                                           .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                         .equals("1-0:1.8.0")))
                    .then(adapter::close)
                    .expectComplete()
                    .verify();

        assertThat(logCaptorAiidaDataSource.getWarnLogs()).contains(
                "Found unknown OBIS-CODES from " + AiidaAsset.CONNECTION_AGREEMENT_POINT + ": [UNKNOWN-OBIS-CODE]");
    }

    @Test
    void givenConnectionLost_warningIsLogged() {
        adapter.disconnected(new MqttDisconnectResponse(new MqttException(998877)));
        assertThat(logCaptor.getWarnLogs()).contains("Disconnected from MQTT broker");
    }
}