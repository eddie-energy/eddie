// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.provisioning.MqttProvisioningConnectionDto;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.models.mqtt.MqttConnection;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.provisioning.ProvisioningMqttPublisher;
import energy.eddie.aiida.repositories.InboundDataSourceRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvisioningServiceTest {
    private static final UUID PERMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String HOST = "mqtt://broker.example.test";
    private static final String USERNAME = "provisioning-user";
    private static final String PASSWORD = "provisioning-password";
    private static final String TOPIC = "aiida/inbound/test";
    private static final String AIIDA_USERNAME = "aiida";
    private static final String AIIDA_PASSWORD = "aiida-password";

    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    @SuppressWarnings("UnusedVariable")
    private InboundDataSourceRepository inboundDataSourceRepository;
    @Mock
    private MqttConfiguration mqttConfiguration;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ProvisioningService service;

    @AfterEach
    void tearDown() {
        service.close();
    }

    @Test
    void changeProvisioningType_withMqttClient_establishesConnectionAndCreatesPublisher()
            throws Exception {
        var dataSource = inboundDataSource();
        var connection = mock(MqttConnection.class);
        var patch = patch(InboundProvisioningType.MQTT_CLIENT);
        var expected = new MqttProvisioningConnectionDto(HOST, USERNAME, PASSWORD, TOPIC);

        when(dataSource.establishClientModeConnection(HOST, USERNAME, PASSWORD, TOPIC)).thenReturn(expected);
        when(dataSource.provisioningConnection()).thenReturn(connection);
        when(dataSource.provisioningTopicOrThrow()).thenReturn(TOPIC);

        try (MockedConstruction<ProvisioningMqttPublisher> publishers =
                     mockConstruction(ProvisioningMqttPublisher.class)) {
            var result = service.changeProvisioningType(PERMISSION_ID, patch);

            assertThat(result).isEqualTo(expected);
            verify(dataSource).establishClientModeConnection(HOST, USERNAME, PASSWORD, TOPIC);
            verify(dataSource).provisioningConnection();
            verify(dataSource).provisioningTopicOrThrow();
            assertThat(publishers.constructed()).hasSize(1);
        }
    }

    @Test
    void changeProvisioningType_withMqttServer_establishesConnectionAndCreatesPublisher()
            throws Exception {
        var dataSource = inboundDataSource();
        var connection = mock(MqttConnection.class);
        var patch = patch(InboundProvisioningType.MQTT_SERVER);
        var expected = new MqttProvisioningConnectionDto(HOST, USERNAME, PASSWORD, TOPIC);
        var generatedPassword = ArgumentCaptor.forClass(String.class);

        when(dataSource.establishServerModeConnection(eq(mqttConfiguration), eq(passwordEncoder), anyString()))
                .thenReturn(expected);
        when(dataSource.provisioningConnection()).thenReturn(connection);
        when(dataSource.provisioningTopicOrThrow()).thenReturn(TOPIC);
        when(dataSource.inboundProvisioningType()).thenReturn(InboundProvisioningType.MQTT_SERVER);
        when(mqttConfiguration.username()).thenReturn(AIIDA_USERNAME);
        when(mqttConfiguration.password()).thenReturn(AIIDA_PASSWORD);

        try (MockedConstruction<ProvisioningMqttPublisher> publishers =
                     mockConstruction(ProvisioningMqttPublisher.class)) {
            var result = service.changeProvisioningType(PERMISSION_ID, patch);

            assertThat(result).isEqualTo(expected);
            verify(dataSource).establishServerModeConnection(
                    eq(mqttConfiguration),
                    eq(passwordEncoder),
                    generatedPassword.capture()
            );
            verify(dataSource).provisioningConnection();
            verify(dataSource).provisioningTopicOrThrow();
            assertThat(publishers.constructed()).hasSize(1);
            assertThat(generatedPassword.getValue()).hasSize(10);
            verify(mqttConfiguration).username();
            verify(mqttConfiguration).password();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialize_restoresClientAndServerPublishers() {
        when(inboundAggregator.inboundRecordFlux()).thenReturn(reactor.core.publisher.Flux.never());
        when(inboundDataSourceRepository.findByProvisioningTypeIn(anySet())).thenReturn(List.of());

        service.initialize();

        var types = ArgumentCaptor.forClass(Set.class);
        verify(inboundDataSourceRepository).findByProvisioningTypeIn(types.capture());
        assertThat(types.getValue()).containsExactlyInAnyOrder(
                InboundProvisioningType.MQTT_CLIENT,
                InboundProvisioningType.MQTT_SERVER
        );
    }

    @Test
    void stopPublisher_closesPublisher() throws Exception {
        var dataSource = inboundDataSource();
        var connection = mock(MqttConnection.class);

        when(dataSource.establishClientModeConnection(HOST, USERNAME, PASSWORD, TOPIC))
                .thenReturn(new MqttProvisioningConnectionDto(HOST, USERNAME, PASSWORD, TOPIC));
        when(dataSource.provisioningConnection()).thenReturn(connection);
        when(dataSource.provisioningTopicOrThrow()).thenReturn(TOPIC);

        try (MockedConstruction<ProvisioningMqttPublisher> publishers =
                     mockConstruction(ProvisioningMqttPublisher.class)) {
            service.changeProvisioningType(PERMISSION_ID, patch(InboundProvisioningType.MQTT_CLIENT));

            service.stopPublisher(DATA_SOURCE_ID);

            verify(publishers.constructed().getFirst()).close();
        }
    }

    @Test
    void changeProvisioningType_withRestBearer_stopsPublisherAndReturnsEmptyConnectionDto()
            throws Exception {
        var dataSource = inboundDataSource();
        var connection = mock(MqttConnection.class);
        var expected = new MqttProvisioningConnectionDto(HOST, USERNAME, PASSWORD, TOPIC);

        when(dataSource.establishClientModeConnection(HOST, USERNAME, PASSWORD, TOPIC)).thenReturn(expected);
        when(dataSource.provisioningConnection()).thenReturn(connection);
        when(dataSource.provisioningTopicOrThrow()).thenReturn(TOPIC);

        try (MockedConstruction<ProvisioningMqttPublisher> publishers =
                     mockConstruction(ProvisioningMqttPublisher.class)) {
            service.changeProvisioningType(PERMISSION_ID, patch(InboundProvisioningType.MQTT_CLIENT));

            var result = service.changeProvisioningType(PERMISSION_ID, patch(InboundProvisioningType.REST_BEARER));

            assertThat(result).isEqualTo(MqttProvisioningConnectionDto.empty());
            verify(dataSource).changeInboundProvisioningType(InboundProvisioningType.REST_BEARER);
            verify(publishers.constructed().getFirst()).close();
        }
    }

    @Test
    void subscribedInboundRecord_isPublishedByDataSourcePublisher() throws Exception {
        var dataSource = inboundDataSource();
        var connection = mock(MqttConnection.class);
        var inboundRecord = mock(InboundRecord.class);
        var records = Sinks.many().unicast().<InboundRecord>onBackpressureBuffer();

        when(dataSource.establishClientModeConnection(HOST, USERNAME, PASSWORD, TOPIC))
                .thenReturn(new MqttProvisioningConnectionDto(HOST, USERNAME, PASSWORD, TOPIC));
        when(dataSource.provisioningConnection()).thenReturn(connection);
        when(dataSource.provisioningTopicOrThrow()).thenReturn(TOPIC);
        when(dataSource.inboundProvisioningType()).thenReturn(InboundProvisioningType.MQTT_CLIENT);
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(inboundAggregator.inboundRecordFlux()).thenReturn(records.asFlux());

        try (MockedConstruction<ProvisioningMqttPublisher> publishers =
                     mockConstruction(ProvisioningMqttPublisher.class)) {
            service.changeProvisioningType(PERMISSION_ID, patch(InboundProvisioningType.MQTT_CLIENT));
            service.subscribeToInboundRecords();
            assertThat(records.tryEmitNext(inboundRecord)).isEqualTo(Sinks.EmitResult.OK);

            verify(publishers.constructed().getFirst(), timeout(1000)).publish(inboundRecord);
        }
    }

    @Test
    void changeProvisioningType_withoutPermission_throwsPermissionNotFoundException() {
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        assertThrows(
                PermissionNotFoundException.class,
                () -> service.changeProvisioningType(PERMISSION_ID, patch(InboundProvisioningType.MQTT_CLIENT))
        );
    }

    @Test
    void changeProvisioningType_withNonInboundDataSource_throwsInvalidDataSourceTypeException() {
        var permission = mock(Permission.class);
        var dataSource = mock(DataSource.class);

        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));
        when(permission.dataSource()).thenReturn(dataSource);

        assertThrows(
                InvalidDataSourceTypeException.class,
                () -> service.changeProvisioningType(PERMISSION_ID, patch(InboundProvisioningType.MQTT_CLIENT))
        );
    }

    private InboundDataSource inboundDataSource() {
        var permission = mock(Permission.class);
        var dataSource = mock(InboundDataSource.class);

        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));
        when(permission.dataSource()).thenReturn(dataSource);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);

        return dataSource;
    }

    private static ProvisioningTypePatchDto patch(InboundProvisioningType type) {
        return new ProvisioningTypePatchDto(PERMISSION_ID, type, HOST, USERNAME, PASSWORD, TOPIC);
    }
}
