// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.models.permission.AcceptedPermission;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.rtd.SchemaFormatterRegistry;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.aiida.streamers.mqtt.MqttStreamer;
import energy.eddie.aiida.utils.MqttFactory;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamerFactoryTest {
    @Mock
    private MqttStreamingConfig streamingConfig;
    @Mock
    private AcceptedPermission permission;
    @Mock
    private MqttAsyncClient mqttClient;
    @Mock
    private FailedToSendRepository failedToSendRepository;
    @Mock
    private SchemaFormatterRegistry schemaFormatterRegistry;
    @Mock
    private PermissionLatestRecordMap permissionLatestRecordMap;
    @Mock
    private SecretsService secretsService;

    @Test
    void getAiidaStreamer_buildsMqttStreamerWithExpandedPersistencePath() throws Exception {
        // Given
        var eddieId = UUID.randomUUID();
        var permissionId = UUID.randomUUID();
        when(streamingConfig.serverUri()).thenReturn("mqtt://broker");
        when(streamingConfig.username()).thenReturn(permissionId);
        when(permission.eddieId()).thenReturn(eddieId);
        when(permission.id()).thenReturn(permissionId);
        when(permission.mqttStreamingConfig()).thenReturn(streamingConfig);
        var mapper = ObjectMapperCreatorUtil.mapper();

        try (MockedStatic<MqttFactory> mqttFactory = Mockito.mockStatic(MqttFactory.class)) {
            mqttFactory.when(() -> MqttFactory.getMqttAsyncClient(any(), any(), any()))
                       .thenReturn(mqttClient);

            var dependencies = new StreamerDependencies(failedToSendRepository,
                                                        mapper,
                                                        schemaFormatterRegistry,
                                                        Sinks.many().multicast().onBackpressureBuffer(),
                                                        permissionLatestRecordMap,
                                                        secretsService,
                                                        new ConnectionStatusEventStream());

            // When
            var streamer = StreamerFactory.getAiidaStreamer(dependencies, permission, Flux.empty());

            // Then
            assertInstanceOf(MqttStreamer.class, streamer);
            mqttFactory.verify(() -> MqttFactory.getMqttAsyncClient(
                    eq("mqtt://broker"),
                    eq(permissionId.toString()),
                    any()));
        }
    }
}
