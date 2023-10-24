package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.TestUtils;
import energy.eddie.aiida.models.record.StringAiidaRecord;
import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.aiida.utils.MqttFactory;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import reactor.test.StepVerifier;

import java.time.Duration;

import static energy.eddie.aiida.utils.MqttConfig.MqttConfigBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OesterreichsEnergieAdapterTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(OesterreichsEnergieAdapter.class);
    private OesterreichsEnergieAdapter adapter;
    private MqttConfig config;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        config = new MqttConfigBuilder("tcp://localhost:1883", "MyTestTopic").build();
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new OesterreichsEnergieAdapter(config, mapper);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void verify_close_disconnectsAndClosesClient_andEmitsCompleteOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);
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
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);
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
        try (LogCaptor logCaptor = LogCaptor.forClass(OesterreichsEnergieAdapter.class)) {
            try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
                var mockClient = mock(MqttAsyncClient.class);
                mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);
                when(mockClient.disconnect(anyLong())).thenThrow(new MqttException(998877));
                when(mockClient.isConnected()).thenReturn(true);

                adapter.start().subscribe();
                adapter.close();

                assertThat(logCaptor.getWarnLogs()).contains("Error while disconnecting or closing MQTT client");
            }
        }
    }

    @Test
    void verify_start_callsConnect() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);

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
        adapter = new OesterreichsEnergieAdapter(config, mapper);

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);

            adapter.start().subscribe();

            verify(config, atLeastOnce()).username();
            verify(config, atLeastOnce()).password();
        }
    }

    @Test
    void givenErrorDuringStart_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);
            when(mockClient.connect(any())).thenThrow(new MqttException(998877));

            StepVerifier.create(adapter.start())
                    .expectErrorMatches(throwable -> ((MqttException) throwable).getReasonCode() == 998877)
                    .verify();
        }
    }

    @Test
    void givenRecordFromMqttBroker_isPublishedOnFlux() {
        var recordJson = "{\"0-0:96.1.0\":{\"value\":\"90296857\"},\"api_version\":\"v1\",\"name\":\"90296857\",\"sma_time\":2370.6}";
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);

            MqttMessage message = new MqttMessage(recordJson.getBytes());

            StepVerifier.create(adapter.start())
                    // call method to simulate arrived message
                    .then(() -> adapter.messageArrived("MyTestTopic", message))
                    .expectNextMatches(received -> received.code().equals("0-0:96.1.0") && received.timestamp().toEpochMilli() == 0L
                            && ((StringAiidaRecord) received).value().equals("90296857"))
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
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);

            adapter.start().subscribe();

            adapter.connectComplete(false, config.serverURI());

            verify(mockClient).subscribe(config.subscribeTopic(), 2);
        }
    }

    @Test
    void givenErrorWhileSubscribing_errorPublishedOnFlux() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any())).thenReturn(mockClient);
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
                .expectNextMatches(aiidaRecord -> aiidaRecord.code().equals("1-0:2.7.0"))
                .then(adapter::close)
                .expectComplete()
                .verifyLater();

        adapter.messageArrived(config.subscribeTopic(), new MqttMessage(invalidJson.getBytes()));
        adapter.messageArrived(config.subscribeTopic(), new MqttMessage(validJson.getBytes()));

        TestUtils.verifyErrorLogStartsWith("Error while deserializing JSON received from adapter. JSON was %s".formatted(invalidJson),
                logCaptor, JsonMappingException.class);

        stepVerifier.verify();
    }

    @Test
    void givenConnectionLost_warningIsLogged() {
        adapter.connectionLost(new MqttException(998877));
        assertThat(logCaptor.getWarnLogs()).contains("Lost connection to MQTT broker");
    }
}