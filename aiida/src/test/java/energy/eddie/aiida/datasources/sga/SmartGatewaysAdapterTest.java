package energy.eddie.aiida.datasources.sga;

import energy.eddie.aiida.utils.MqttConfig;
import energy.eddie.aiida.utils.MqttFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SmartGatewaysAdapterTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(SmartGatewaysAdapter.class);
    private SmartGatewaysAdapter adapter;
    private MqttConfig config;
    private static final String testMessageTariff1 = """
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
    private static final String testMessageTariff2 = """
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

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        config = new MqttConfig.MqttConfigBuilder("tcp://localhost:1883", "sga/data").build();
        adapter = new SmartGatewaysAdapter("1", config);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
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
    void givenUsernameAndPassword_isUsedByAdapter() {
        config = new MqttConfig.MqttConfigBuilder("tcp://localhost:1883", "sga/data")
                .setUsername("User")
                .setPassword("Pass")
                .build();
        config = spy(config);
        adapter = new SmartGatewaysAdapter("1", config);

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
    void givenRecordFromMqttBroker_isPublishedOnFluxTariff1() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(testMessageTariff1.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("sga/data", message))
                        .expectNextMatches(received -> received.aiidaRecordValue().stream()
                                                               .anyMatch(aiidaRecordValue -> (aiidaRecordValue.dataTag()
                                                                                                             .equals("1-0:1.7.0")
                                                                                             && aiidaRecordValue.value()
                                                                                                                .equals("0.445"))
                                                                                             || (aiidaRecordValue.dataTag()
                                                                                                                 .equals("1-0:1.8.0")
                                                                                                 && aiidaRecordValue.value()
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

            MqttMessage message = new MqttMessage(testMessageTariff2.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived("sga/data", message))
                        .expectNextMatches(received -> received.aiidaRecordValue().stream()
                                                               .anyMatch(aiidaRecordValue -> (aiidaRecordValue.dataTag()
                                                                                                             .equals("1-0:1.7.0")
                                                                                             && aiidaRecordValue.value()
                                                                                                                .equals("0.030"))
                                                                                             || (aiidaRecordValue.dataTag()
                                                                                                                 .equals("1-0:1.8.0")
                                                                                                 && aiidaRecordValue.value()
                                                                                                                    .equals("1894.882"))))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }
}
