// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptedPermissionTest {
    private static final UUID EDDIE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID PERMISSION_ID = UUID.fromString("a4dc1bad-b9fe-47ae-9336-690cfb4aada9");
    private static final UUID USER_ID = UUID.fromString("6b2a2c5e-0f5e-4b53-9d6b-7f2f8f5f1c11");
    private static final Instant START = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant EXPIRATION = Instant.parse("2026-12-01T00:00:00Z");
    private static final CronExpression SCHEDULE = CronExpression.parse("*/5 * * * * *");
    @Mock
    private OutboundAiidaLocalDataNeed dataNeed;
    @Mock
    private InboundAiidaLocalDataNeed inboundDataNeed;
    @Mock
    private DataSource dataSource;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private MqttStreamingConfig mqttStreamingConfig;

    @Test
    void from_acceptedAndCompletePermission_copiesAllFields() {
        // Given
        var permission = completePermission();
        when(dataNeed.transmissionSchedule()).thenReturn(SCHEDULE);

        // When
        var acceptedPermission = AcceptedPermission.from(permission);

        // Then
        assertThat(acceptedPermission.id()).isEqualTo(PERMISSION_ID);
        assertThat(acceptedPermission.eddieId()).isEqualTo(EDDIE_ID);
        assertThat(acceptedPermission.userId()).isEqualTo(USER_ID);
        assertThat(acceptedPermission.connectionId()).isEqualTo("connection-id");
        assertThat(acceptedPermission.startTime()).isEqualTo(START);
        assertThat(acceptedPermission.expirationTime()).isEqualTo(EXPIRATION);
        assertThat(acceptedPermission.dataNeed()).isSameAs(dataNeed);
        assertThat(acceptedPermission.dataSource()).isSameAs(dataSource);
        assertThat(acceptedPermission.mqttStreamingConfig()).isSameAs(mqttStreamingConfig);
        assertThat(acceptedPermission.transmissionSchedule()).isEqualTo(SCHEDULE);
        assertThat(acceptedPermission.transmissionEnabled()).isTrue();
    }

    @Test
    void from_permissionWithOwnTransmissionSchedule_usesItInsteadOfTheDataNeedSchedule() {
        // Given
        var permission = completePermission();
        var ownSchedule = CronExpression.parse("0 * * * * *");
        permission.setTransmissionSchedule(ownSchedule);

        // When
        var acceptedPermission = AcceptedPermission.from(permission);

        // Then
        assertThat(acceptedPermission.transmissionSchedule()).isEqualTo(ownSchedule);
    }

    @Test
    void from_revokedPermission_throws() {
        // Given
        var permission = completePermission();
        permission.setStatus(PermissionStatus.REVOKED);

        // When, Then
        assertThatThrownBy(() -> AcceptedPermission.from(permission))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not accepted");
    }

    @Test
    void from_permissionWithoutDataSource_throws() {
        // Given
        var permission = completePermission();
        permission.setDataSource(null);

        // When, Then
        assertThatThrownBy(() -> AcceptedPermission.from(permission))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Accepted permission %s is missing its data source", PERMISSION_ID);
    }

    @Test
    void from_permissionWithoutMqttStreamingConfig_throws() {
        // Given
        var permission = new OutboundPermission(EDDIE_ID,
                                                PERMISSION_ID,
                                                "https://example.org",
                                                "token",
                                                USER_ID,
                                                dataNeed,
                                                "connection-id",
                                                START,
                                                EXPIRATION,
                                                Instant.parse("2026-01-01T00:00:00Z"));
        permission.setDataSource(dataSource);

        // When, Then
        assertThatThrownBy(() -> AcceptedPermission.from(permission))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Accepted permission %s is missing its MQTT streaming config", PERMISSION_ID);
    }

    @Test
    void from_acceptedInboundPermission_createsInboundSnapshot() {
        // Given
        var permission = new InboundPermission(EDDIE_ID,
                                               PERMISSION_ID,
                                               "https://example.org",
                                               "token",
                                               USER_ID,
                                               inboundDataNeed,
                                               "connection-id",
                                               START,
                                               EXPIRATION,
                                               Instant.parse("2026-01-01T00:00:00Z"));
        permission.setDataSource(inboundDataSource);
        permission.setMqttStreamingConfig(mqttStreamingConfig);

        // When
        AcceptedPermission acceptedPermission = AcceptedPermission.from((Permission) permission);

        // Then
        assertThat(acceptedPermission).isInstanceOf(AcceptedInboundPermission.class);
        assertThat(acceptedPermission.id()).isEqualTo(PERMISSION_ID);
        assertThat(acceptedPermission.dataNeed()).isSameAs(inboundDataNeed);
        assertThat(acceptedPermission.dataSource()).isSameAs(inboundDataSource);
        assertThat(acceptedPermission.mqttStreamingConfig()).isSameAs(mqttStreamingConfig);
    }

    @Test
    void from_acceptedOutboundPermissionAsPermission_createsOutboundSnapshot() {
        // Given
        Permission permission = completePermission();
        when(dataNeed.transmissionSchedule()).thenReturn(SCHEDULE);

        // When
        var acceptedPermission = AcceptedPermission.from(permission);

        // Then
        assertThat(acceptedPermission).isInstanceOf(AcceptedOutboundPermission.class);
    }

    private OutboundPermission completePermission() {
        var permission = new OutboundPermission(EDDIE_ID,
                                                PERMISSION_ID,
                                                "https://example.org",
                                                "token",
                                                USER_ID,
                                                dataNeed,
                                                "connection-id",
                                                START,
                                                EXPIRATION,
                                                Instant.parse("2026-01-01T00:00:00Z"));
        permission.setDataSource(dataSource);
        permission.setMqttStreamingConfig(mqttStreamingConfig);
        return permission;
    }
}
