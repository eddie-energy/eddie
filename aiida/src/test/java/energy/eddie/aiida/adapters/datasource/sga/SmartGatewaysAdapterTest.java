package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceIcon;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysDataSource;
import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysTopic;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
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
import java.util.UUID;

import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SmartGatewaysAdapterTest {

    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");

    private static final SmartGatewaysDataSource DATA_SOURCE = new SmartGatewaysDataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.SMART_GATEWAYS_ADAPTER,
                              AiidaAsset.SUBMETER,
                              "sma",
                              "AT",
                              true,
                              DataSourceIcon.METER,
                              null,
                              null,
                              null),
            USER_ID,
            new DataSourceMqttDto("tcp://localhost:1883", "tcp://localhost:1883", "aiida/test", "user", "password")
    );

    private SmartGatewaysAdapter adapter;
    private MqttConfiguration mqttConfiguration;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));
        mqttConfiguration = new MqttConfiguration(
                "tcp://localhost:1883",
                "tcp://localhost:1883",
                10,
                "password",
                ""
        );
        adapter = new SmartGatewaysAdapter(DATA_SOURCE, mqttConfiguration);
    }

    @AfterEach
    void tearDown() {
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

            StepVerifier.create(adapter.start()).then(() -> {
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
                                                                                && v.value().equals("45"))
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

    private void send(String topicSuffix, String value) {
        String topic = "aiida/" + topicSuffix;
        MqttMessage message = new MqttMessage(value.getBytes(StandardCharsets.UTF_8));
        adapter.messageArrived(topic, message);
    }
}
