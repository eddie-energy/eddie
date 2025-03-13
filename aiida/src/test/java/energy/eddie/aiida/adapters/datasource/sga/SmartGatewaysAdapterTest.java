package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.sga.SmartGatewaysDataSource;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmartGatewaysAdapterTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(SmartGatewaysAdapter.class);
    private static final String TEST_MESSAGE_TARIFF_1 = """
            4530303632303030303037393239333232
            4730303732303034303036353733323230
            0001
            3845.467
            2621.303
            1894.882
            1435.228
            0.000
            0.000
            0.000
            0.000
            0.531
            0.445
            0
            71
            460
            30
            0
            0
            233.00
            234.00
            234.00
            0.00
            0.00
            2.00
            736.650
            -0.061
            0.002""";
    private static final String TEST_MESSAGE_TARIFF_2 = """
            4530303632303030303037393239333232
            4730303732303034303036353733323230
            0002
            3845.467
            2621.303
            1894.882
            1435.228
            0.000
            0.000
            0.000
            0.000
            0.531
            0.030
            0
            71
            460
            30
            0
            0
            233.00
            234.00
            234.00
            0.00
            0.00
            2.00
            736.650
            -0.061
            0.002""";
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final SmartGatewaysDataSource DATA_SOURCE = new SmartGatewaysDataSource(
            new DataSourceDto(DATA_SOURCE_ID,
                              DataSourceType.Identifiers.SMART_GATEWAYS,
                              AiidaAsset.SUBMETER.asset(),
                              "sma",
                              true,
                              null,
                              null,
                              null),
            USER_ID,
            new DataSourceMqttDto("tcp://localhost:1883",
                                  "aiida/test",
                                  "user",
                                  "password")
    );
    private SmartGatewaysAdapter adapter;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        adapter = new SmartGatewaysAdapter(DATA_SOURCE);
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
        try (LogCaptor logCaptor = LogCaptor.forClass(SmartGatewaysAdapter.class)) {
            try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
                var mockClient = mock(MqttAsyncClient.class);
                mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                               .thenReturn(mockClient);
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
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            adapter.start().subscribe();

            verify(mockClient).connect(any());
        }
    }

    @Test
    void verify_usernameAndPassword_isUsedByAdapter() {
        var spiedDataSource = spy(DATA_SOURCE);
        adapter = new SmartGatewaysAdapter(spiedDataSource);

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
    void givenRecordFromMqttBroker_isPublishedOnFluxTariff1() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(TEST_MESSAGE_TARIFF_1.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("sga/data", message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue -> (
                                                                                                     aiidaRecordValue.dataTag()
                                                                                                                     .equals(POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                                     aiidaRecordValue.value()
                                                                                                                     .equals("0.445")) ||
                                                                                             (aiidaRecordValue.dataTag()
                                                                                                              .equals(POSITIVE_ACTIVE_ENERGY) && aiidaRecordValue.value()
                                                                                                                                                                 .equals("3845.467"))))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }

    @Test
    void givenRecordFromMqttBroker_isPublishedOnFluxTariff2() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(TEST_MESSAGE_TARIFF_2.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("sga/data", message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue -> (aiidaRecordValue.dataTag()
                                                                                                              .equals(POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                              aiidaRecordValue.value()
                                                                                                              .equals("0.030")) ||
                                                                                             (aiidaRecordValue.dataTag()
                                                                                                              .equals(POSITIVE_ACTIVE_ENERGY) &&
                                                                                              aiidaRecordValue.value()
                                                                                                              .equals("1894.882"))))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }
}
