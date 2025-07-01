package energy.eddie.aiida.adapters.datasource.fr;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3Mode;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3ModeNotSupportedException;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.fr.MicroTeleinfoV3DataSource;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.aiida.utils.ObisCode;
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
import java.util.Objects;
import java.util.UUID;

import static energy.eddie.aiida.utils.ObisCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MicroTeleinfoV3AdapterTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(MicroTeleinfoV3Adapter.class);
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final MicroTeleinfoV3DataSource DATA_SOURCE = new MicroTeleinfoV3DataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.MICRO_TELEINFO,
                              AiidaAsset.SUBMETER,
                              "teleinfo",
                              "FR",
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
    private MicroTeleinfoV3Adapter adapter;
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
                "user",
                "password",
                ""
        );
        adapter = new MicroTeleinfoV3Adapter(DATA_SOURCE, mapper, mqttConfiguration);

        LOG_CAPTOR.resetLogLevel();
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
                                                                                                              .equals(METER_SERIAL) &&
                                                                                              aiidaRecordValue.value()
                                                                                                              .equals("123456789123")) ||
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
        var validJson = "{\"ADCO\":{\"raw\":\"123456789123\",\"value\":123456789123},\"OPTARIF\":{\"raw\":\"BASE\",\"value\":\"BASE\"},\"ISOUSC\":{\"raw\":\"30\",\"value\":30},\"BASE\":{\"raw\":\"006367621\",\"value\":6367621},\"PTEC\":{\"raw\":\"TH..\",\"value\":\"TH\"},\"IINST\":{\"raw\":\"001\",\"value\":1},\"IMAX\":{\"raw\":\"090\",\"value\":90},\"PAPP\":{\"raw\":\"00126\",\"value\":126},\"HHPHC\":{\"raw\":\"A\",\"value\":\"A\"}}";

        StepVerifier stepVerifier = StepVerifier.create(adapter.start())
                                                .expectNextMatches(received -> received.aiidaRecordValues()
                                                                                       .stream()
                                                                                       .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                                                     .equals(POSITIVE_ACTIVE_INSTANTANEOUS_POWER) || aiidaRecordValue.dataTag()
                                                                                                                                                                                                     .equals(POSITIVE_ACTIVE_ENERGY)))
                                                .then(adapter::close)
                                                .expectComplete()
                                                .verifyLater();

        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(invalidJson.getBytes(StandardCharsets.UTF_8)));
        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(validJson.getBytes(StandardCharsets.UTF_8)));

        TestUtils.verifyErrorLogStartsWith("Error while deserializing JSON received from adapter. JSON was %s".formatted(
                invalidJson), LOG_CAPTOR, MicroTeleinfoV3ModeNotSupportedException.class);

        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenJsonIsFromHistoryMode() {
        LOG_CAPTOR.setLogLevelToDebug();
        var historyModeJson = "{\"ADCO\":{\"raw\":\"123456789123\",\"value\":123456789123},\"OPTARIF\":{\"raw\":\"BASE\",\"value\":\"BASE\"},\"ISOUSC\":{\"raw\":\"30\",\"value\":30},\"BASE\":{\"raw\":\"006367621\",\"value\":6367621},\"PTEC\":{\"raw\":\"TH..\",\"value\":\"TH\"},\"IINST\":{\"raw\":\"001\",\"value\":1},\"IMAX\":{\"raw\":\"090\",\"value\":90},\"PAPP\":{\"raw\":\"00126\",\"value\":126},\"HHPHC\":{\"raw\":\"A\",\"value\":\"A\"}}";

        StepVerifier stepVerifier = StepVerifier.create(adapter.start())
                                                .expectNextMatches(received -> received.aiidaRecordValues()
                                                                                       .stream()
                                                                                       .anyMatch(aiidaRecordValue -> aiidaRecordValue.dataTag()
                                                                                                                                     .equals(ObisCode.METER_SERIAL) || aiidaRecordValue.dataTag()
                                                                                                                                                                                       .equals(POSITIVE_ACTIVE_ENERGY)))
                                                .then(adapter::close)
                                                .expectComplete()
                                                .verifyLater();

        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(historyModeJson.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, LOG_CAPTOR.getDebugLogs().size());
        assertTrue(LOG_CAPTOR.getDebugLogs().contains("Connected smart meter operates in %s mode.".formatted(
                MicroTeleinfoV3Mode.HISTORY)));
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenJsonIsFromStandardMode() {
        LOG_CAPTOR.setLogLevelToDebug();
        var standardModeJson = """
                {
                  "ADSC": {
                    "raw": "841875104423",
                    "value": 841875104423
                  },
                  "VTIC": {
                    "raw": "02",
                    "value": 2
                  },
                  "DATE": {
                    "raw": "",
                    "value": "",
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:54:38.000Z"
                    }
                  },
                  "NGTF": {
                    "raw": "      BASE      ",
                    "value": "      BASE      "
                  },
                  "LTARF": {
                    "raw": "      BASE      ",
                    "value": "      BASE      "
                  },
                  "EAST": {
                    "raw": "032507388",
                    "value": 32507388
                  },
                  "EASF01": {
                    "raw": "032507388",
                    "value": 32507388
                  },
                  "EASF03": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF04": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF05": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF06": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF07": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF08": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF09": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF10": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASD01": {
                    "raw": "007545288",
                    "value": 7545288
                  },
                  "EASD02": {
                    "raw": "006699887",
                    "value": 6699887
                  },
                  "EASD04": {
                    "raw": "011806842",
                    "value": 11806842
                  },
                  "EAIT": {
                    "raw": "009509008",
                    "value": 9509008
                  },
                  "ERQ1": {
                    "raw": "003136793",
                    "value": 3136793
                  },
                  "ERQ2": {
                    "raw": "000015124",
                    "value": 15124
                  },
                  "ERQ3": {
                    "raw": "001432483",
                    "value": 1432483
                  },
                  "ERQ4": {
                    "raw": "006198829",
                    "value": 6198829
                  },
                  "IRMS1": {
                    "raw": "005",
                    "value": 5
                  },
                  "URMS1": {
                    "raw": "236",
                    "value": 236
                  },
                  "PREF": {
                    "raw": "12",
                    "value": 12
                  },
                  "PCOUP": {
                    "raw": "12",
                    "value": 12
                  },
                  "SINSTS": {
                    "raw": "00000",
                    "value": 0
                  },
                  "SMAXSN-1": {
                    "raw": "07000",
                    "value": 7000,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-08T06:11:26.000Z"
                    }
                  },
                  "SINSTI": {
                    "raw": "01156",
                    "value": 1156
                  },
                  "SMAXIN": {
                    "raw": "05030",
                    "value": 5030,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:45:12.000Z"
                    }
                  },
                  "SMAXIN-1": {
                    "raw": "04140",
                    "value": 4140,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-08T12:53:25.000Z"
                    }
                  },
                  "CCASN": {
                    "raw": "00000",
                    "value": 0,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:30:00.000Z"
                    }
                  },
                  "CCASN-1": {
                    "raw": "00000",
                    "value": 0,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:00:00.000Z"
                    }
                  },
                  "CCAIN": {
                    "raw": "02556",
                    "value": 2556,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:30:00.000Z"
                    }
                  },
                  "CCAIN-1": {
                    "raw": "03582",
                    "value": 3582,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:00:00.000Z"
                    }
                  },
                  "UMOY1": {
                    "raw": "237",
                    "value": 237,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:50:00.000Z"
                    }
                  },
                  "STGE": {
                    "raw": "003A4301",
                    "value": "003A4301"
                  },
                  "MSG1": {
                    "raw": "PAS DE          MESSAGE     \\u0000   ",
                    "value": "PAS DE          MESSAGE     \\u0000   "
                  },
                  "PRM": {
                    "raw": "06444138907938",
                    "value": 6444138907938
                  },
                  "RELAIS": {
                    "raw": "000",
                    "value": 0
                  },
                  "NTARF": {
                    "raw": "01",
                    "value": 1
                  },
                  "NJOURF": {
                    "raw": "00",
                    "value": 0
                  },
                  "NJOURF+1": {
                    "raw": "00",
                    "value": 0
                  }
                }
                """;
        StepVerifier stepVerifier = StepVerifier.create(adapter.start())
                                                .expectNextMatches(received -> {
                                                    var aiidaRecordValues = received.aiidaRecordValues();
                                                    var hasPositiveActiveTags = aiidaRecordValues.stream()
                                                            .anyMatch(aiidaRecordValue -> Objects.equals(
                                                                    aiidaRecordValue.dataTag(),
                                                                    POSITIVE_ACTIVE_INSTANTANEOUS_POWER) || Objects.equals(
                                                                    aiidaRecordValue.dataTag(),
                                                                    POSITIVE_ACTIVE_ENERGY));

                                                    var hasMeterDeviceId = aiidaRecordValues.stream()
                                                            .anyMatch(aiidaRecordValue -> Objects.equals(
                                                                    aiidaRecordValue.dataTag(),
                                                                    DEVICE_ID_1));

                                                    return hasPositiveActiveTags && hasMeterDeviceId;
                                                })
                                                .then(adapter::close)
                                                .expectComplete()
                                                .verifyLater();

        adapter.messageArrived(DATA_SOURCE.mqttSubscribeTopic(),
                               new MqttMessage(standardModeJson.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, LOG_CAPTOR.getDebugLogs().size());
        assertTrue(LOG_CAPTOR.getDebugLogs().contains("Connected smart meter operates in %s mode.".formatted(
                MicroTeleinfoV3Mode.STANDARD)));
        stepVerifier.verify(Duration.ofSeconds(2));
    }

    @Test
    void givenConnectionLost_warningIsLogged() {
        adapter.disconnected(new MqttDisconnectResponse(new MqttException(998877)));
        assertThat(LOG_CAPTOR.getWarnLogs()).contains("Disconnected from MQTT broker");
    }
}
