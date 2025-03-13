package energy.eddie.aiida.adapters.datasource.fr;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.fr.MicroTeleinfoV3DataSource;
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

import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MicroTeleinfoV3AdapterTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(MicroTeleinfoV3Adapter.class);
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final MicroTeleinfoV3DataSource DATA_SOURCE = new MicroTeleinfoV3DataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.Identifiers.MICRO_TELEINFO,
                              AiidaAsset.SUBMETER.asset(),
                              "teleinfo",
                              true,
                              "FR123456789123",
                              null,
                              null),
            USER_ID,
            new DataSourceMqttDto("tcp://localhost:1883",
                                  "aiida/test",
                                  "user",
                                  "password")
    );
    private MicroTeleinfoV3Adapter adapter;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        mapper = new AiidaConfiguration().objectMapper();
        adapter = new MicroTeleinfoV3Adapter(DATA_SOURCE, mapper);
    }

    @AfterEach
    void tearDown() {
        LOG_CAPTOR.clearLogs();
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
        try (LogCaptor captor = LogCaptor.forClass(MicroTeleinfoV3Adapter.class)) {
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
    void verify_usernameAndPassword_isUsedByAdapter() {
        var spiedDataSource = spy(DATA_SOURCE);
        adapter = new MicroTeleinfoV3Adapter(spiedDataSource, mapper);

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            adapter.start().subscribe();

            verify(spiedDataSource, atLeastOnce()).mqttUsername();
            verify(spiedDataSource, atLeastOnce()).mqttPassword();
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
    void givenRecordFromMqttBroker_isPublishedOnFlux() {
        var recordJson = "{\"ADCO\":{\"raw\":\"123456789123\",\"value\":123456789123},\"OPTARIF\":{\"raw\":\"BASE\",\"value\":\"BASE\"},\"ISOUSC\":{\"raw\":\"30\",\"value\":30},\"BASE\":{\"raw\":\"006367621\",\"value\":6367621},\"PTEC\":{\"raw\":\"TH..\",\"value\":\"TH\"},\"IINST\":{\"raw\":\"001\",\"value\":1},\"IMAX\":{\"raw\":\"090\",\"value\":90},\"PAPP\":{\"raw\":\"00126\",\"value\":126},\"HHPHC\":{\"raw\":\"A\",\"value\":\"A\"}}";

        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(recordJson.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("teleinfo/data", message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue -> (aiidaRecordValue.dataTag()
                                                                                                              .equals(POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                              aiidaRecordValue.value()
                                                                                                              .equals("126")) ||
                                                                                             (aiidaRecordValue.dataTag()
                                                                                                              .equals(POSITIVE_ACTIVE_ENERGY) &&
                                                                                              aiidaRecordValue.value()
                                                                                                              .equals("6367621"))))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenStatusFromMqttBroker_HealthUp() {
        var status = "up";
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(true);

            MqttMessage message = new MqttMessage(status.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("teleinfo/status", message))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();

            assertEquals(Status.UP, adapter.health().getStatus());
        }
    }


    @Test
    void givenStatusFromMqttBroker_HealthDown() {
        var status = "down";
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);
            when(mockClient.isConnected()).thenReturn(true);

            MqttMessage message = new MqttMessage(status.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("teleinfo/status", message))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();

            assertEquals(Status.DOWN, adapter.health().getStatus());
        }
    }

    @Test
    void givenConnectComplete_subscribesToTopic() throws MqttException {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            adapter.start().subscribe();

            adapter.connectComplete(false, DATA_SOURCE.mqttServerUri());

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
                        .then(() -> adapter.connectComplete(false, DATA_SOURCE.mqttServerUri()))
                        .expectError()
                        .verify();
        }
    }

    @Test
    void givenInvalidJsonMessage_errorIsLogged() {
        var invalidJson = "{\"foo\":\"bar\"}";
        var validJson = "{\"ADCO\":{\"raw\":\"123456789123\",\"value\":123456789123},\"OPTARIF\":{\"raw\":\"BASE\",\"value\":\"BASE\"},\"ISOUSC\":{\"raw\":\"30\",\"value\":30},\"BASE\":{\"raw\":\"006367621\",\"value\":6367621},\"PTEC\":{\"raw\":\"TH..\",\"value\":\"TH\"},\"IINST\":{\"raw\":\"001\",\"value\":1},\"IMAX\":{\"raw\":\"090\",\"value\":90},\"PAPP\":{\"raw\":\"00126\",\"value\":126},\"HHPHC\":{\"raw\":\"A\",\"value\":\"A\"}}";

        StepVerifier stepVerifier = StepVerifier.create(adapter.start())
                                                .expectNextMatches(received -> received.aiidaRecordValues()
                                                                                       .stream()
                                                                                       .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                                                     .equals(POSITIVE_ACTIVE_INSTANTANEOUS_POWER) ||
                                                                                                                     aiidaRecordValue.dataTag()
                                                                                                                                     .equals(POSITIVE_ACTIVE_ENERGY)))
                                                .then(adapter::close)
                                                .expectComplete()
                                                .verifyLater();

        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(invalidJson.getBytes(StandardCharsets.UTF_8)));
        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(validJson.getBytes(StandardCharsets.UTF_8)));

        TestUtils.verifyErrorLogStartsWith("Error while deserializing JSON received from adapter. JSON was %s".formatted(
                invalidJson), LOG_CAPTOR, JsonMappingException.class);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenConnectionLost_warningIsLogged() {
        adapter.disconnected(new MqttDisconnectResponse(new MqttException(998877)));
        assertThat(LOG_CAPTOR.getWarnLogs()).contains("Disconnected from MQTT broker");
    }
}
