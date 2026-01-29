// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.it;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.adapters.datasource.it.transformer.SinapsiAlfaEntryJsonTest;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.utils.MqttFactory;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SinapsiAlfaAdapterTest {
    private static final String TOPIC = "/oetzi/iomtsgdata/abcdef-abcde-abcde-abcde-12345/";
    private static final SinapsiAlfaDataSource DATA_SOURCE = mock(SinapsiAlfaDataSource.class);
    private static final MqttConfiguration MQTT_CONFIGURATION = mock(MqttConfiguration.class);
    private SinapsiAlfaAdapter adapter;

    @BeforeEach
    void setUp() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(1));

        when(DATA_SOURCE.internalHost()).thenReturn("tcp://localhost:1883");
        when(DATA_SOURCE.topic()).thenReturn(TOPIC);
        when(DATA_SOURCE.username()).thenReturn("oetzi");
        when(DATA_SOURCE.password()).thenReturn("other-password");
        when(DATA_SOURCE.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(MQTT_CONFIGURATION.password()).thenReturn("password");

        var mapper = ObjectMapperCreatorUtil.mapper();
        adapter = new SinapsiAlfaAdapter(DATA_SOURCE, mapper, MQTT_CONFIGURATION);
    }

    @Test
    void givenPayloadFromMqttBroker_isPublishedOnFlux() {
        try (MockedStatic<MqttFactory> mockMqttFactory = mockStatic(MqttFactory.class)) {
            var mockClient = mock(MqttAsyncClient.class);
            mockMqttFactory.when(() -> MqttFactory.getMqttAsyncClient(anyString(), anyString(), any()))
                           .thenReturn(mockClient);

            MqttMessage message = new MqttMessage(SinapsiAlfaEntryJsonTest.PAYLOAD.getBytes(StandardCharsets.UTF_8));

            StepVerifier.create(adapter.start())
                        // call method to simulate arrived message
                        .then(() -> adapter.messageArrived(TOPIC, message))
                        .expectNextMatches(received -> received.aiidaRecordValues()
                                                               .stream()
                                                               .anyMatch(aiidaRecordValue ->
                                                                                 aiidaRecordValue.dataTag()
                                                                                                 .equals(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER) &&
                                                                                 aiidaRecordValue.value()
                                                                                                 .equals("0.059")
                                                               ))
                        .then(adapter::close)
                        .expectComplete()
                        .log()
                        .verify();
        }
    }
}
