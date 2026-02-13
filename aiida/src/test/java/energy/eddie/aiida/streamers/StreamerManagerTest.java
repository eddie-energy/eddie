// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.aggregator.Aggregator;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.SchemaFormatterRegistry;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.ObisCode;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamerManagerTest {
    private final UUID eddieId = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID permissionId = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID userId = UUID.fromString("40201e2c-a01c-41b8-9db6-3f51670df7a5");
    private final Instant grant = Instant.parse("2023-08-01T10:00:00.00Z");
    private final Instant start = grant.plusSeconds(100_000);
    private final Instant expirationTime = start.plusSeconds(800_000);
    @Mock
    private Aggregator aggregatorMock;
    @Mock
    private FailedToSendRepository mockRepository;
    @Mock
    private AiidaConnectionStatusMessageDto mockStatusMessage;
    @Mock
    private Permission mockPermission;
    @Mock
    private DataSource mockDataSource;
    private StreamerManager manager;
    @Mock
    private AiidaLocalDataNeed mockDataNeed;
    @Mock
    private AiidaStreamer mockAiidaStreamer;
    @Mock
    private Map<UUID, AiidaStreamer> mockMap;
    @Mock
    private PermissionLatestRecordMap mockLatestRecordMap;
    @Mock
    private SchemaFormatterRegistry schemaFormatterRegistry;

    @BeforeEach
    void setUp() {
        var mapper = ObjectMapperCreatorUtil.mapper();

        manager = new StreamerManager(aggregatorMock,
                                      mockRepository,
                                      mapper,
                                      schemaFormatterRegistry,
                                      mockLatestRecordMap);
    }

    @Test
    void givenSamePermissionTwice_createNewStreamer_throws() {
        // Given
        when(aggregatorMock.getFilteredFlux(any(), any(), any(), any(), any(), any())).thenReturn(Flux.empty());
        when(mockPermission.eddieId()).thenReturn(eddieId);
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.expirationTime()).thenReturn(expirationTime);
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);
        when(mockPermission.userId()).thenReturn(userId);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);
        when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY));
        when(mockDataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("* * * * * *"));
        try (MockedStatic<StreamerFactory> utilities = Mockito.mockStatic(StreamerFactory.class)) {
            utilities.when(() -> StreamerFactory.getAiidaStreamer(any(), any(), any(), any(), any(), any(), any()))
                     .thenReturn(mockAiidaStreamer);

            // first time should result in valid creation
            assertDoesNotThrow(() -> manager.createNewStreamer(mockPermission));

            // When, Then
            var thrown = assertThrows(IllegalStateException.class, () -> manager.createNewStreamer(mockPermission));
            assertThat(thrown.getMessage(), startsWith("An AiidaStreamer for EDDIE "));
        }
    }

    @Test
    void givenPermissionWithoutDataSource_createNoStreamer() throws MqttException {
        // Given
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.expirationTime()).thenReturn(expirationTime);
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);
        when(mockPermission.userId()).thenReturn(userId);
        when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY));
        when(mockDataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("* * * * * *"));

        // When
        manager.createNewStreamer(mockPermission);

        // Then
        verify(aggregatorMock, never()).getFilteredFlux(any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenPermission_createStreamer_callsConnect() throws MqttException {
        // Given
        when(aggregatorMock.getFilteredFlux(any(), any(), any(), any(), any(), any())).thenReturn(Flux.empty());
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.expirationTime()).thenReturn(expirationTime);
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);
        when(mockPermission.userId()).thenReturn(userId);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);
        when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY));
        when(mockDataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("* * * * * *"));
        try (MockedStatic<StreamerFactory> utilities = Mockito.mockStatic(StreamerFactory.class)) {
            utilities.when(() -> StreamerFactory.getAiidaStreamer(any(), any(), any(), any(), any(), any(), any()))
                     .thenReturn(mockAiidaStreamer);

            // When
            manager.createNewStreamer(mockPermission);

            // Then
            verify(mockAiidaStreamer).connect();
        }
    }

    @Test
    void givenInvalidPermissionId_stopStreamer_throws() {
        // Given
        when(mockStatusMessage.permissionId()).thenReturn(UUID.fromString("62831e2c-a01c-41b8-9db6-3f51670df7a5"));

        // When, Then
        assertThrows(IllegalArgumentException.class, () -> manager.stopStreamer(mockStatusMessage));
    }

    @Test
    void verify_stopStreamer_closesStreamerTerminally() {
        // Given
        ReflectionTestUtils.setField(manager, "streamers", mockMap);
        when(mockStatusMessage.permissionId()).thenReturn(permissionId);
        when(mockMap.get(permissionId)).thenReturn(mockAiidaStreamer);

        // When
        manager.stopStreamer(mockStatusMessage);

        // Then
        verify(mockAiidaStreamer).closeTerminally(any());
    }

    @Test
    void verify_close_closesAllStreamers() throws MqttException {
        when(aggregatorMock.getFilteredFlux(any(), any(), any(), any(), any(), any())).thenReturn(Flux.empty());
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.expirationTime()).thenReturn(expirationTime);
        when(mockPermission.dataNeed()).thenReturn(mockDataNeed);
        when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY));
        when(mockDataNeed.asset()).thenReturn(AiidaAsset.SUBMETER);
        when(mockPermission.userId()).thenReturn(userId);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);
        when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("* * * * * *"));
        when(aggregatorMock.getFilteredFlux(any(), any(), any(), any(), any(), any())).thenReturn(Flux.empty());
        try (MockedStatic<StreamerFactory> utilities = Mockito.mockStatic(StreamerFactory.class)) {
            utilities.when(() -> StreamerFactory.getAiidaStreamer(any(), any(), any(), any(), any(), any(), any()))
                     .thenReturn(mockAiidaStreamer);

            manager.createNewStreamer(mockPermission);

            // When
            manager.close();

            // Then
            verify(mockAiidaStreamer).close();
        }
    }

    @Test
    void verify_close_emitsCompleteOnTerminationRequestsFlux() {
        StepVerifier.create(manager.terminationRequestsFlux())
                    // When
                    .then(() -> manager.close())
                    // Then
                    .verifyComplete();
    }
}
