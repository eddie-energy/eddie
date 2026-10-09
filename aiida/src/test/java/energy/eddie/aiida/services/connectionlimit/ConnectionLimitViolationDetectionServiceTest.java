// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.ConnectionLimitViolationRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitViolationDetectionServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");
    private static final Instant TIMESTAMP = Instant.parse("2026-07-10T10:00:00Z");
    private static final Instant CLOCK_INSTANT = Instant.parse("2026-07-10T11:00:00Z");
    private final List<ConnectionLimitViolation> storedViolations = new ArrayList<>();
    @Mock
    private AiidaRecordRepository aiidaRecordRepository;
    @Mock
    private ConnectionLimitViolationRepository connectionLimitViolationRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private ConnectionLimitService connectionLimitService;
    @Mock
    private ConnectionLimitNotificationService notificationService;
    @Mock
    private Permission permission;
    private ConnectionLimitViolationDetectionService service;

    @BeforeEach
    void setUp() {
        lenient().when(connectionLimitViolationRepository.save(any(ConnectionLimitViolation.class)))
                 .thenAnswer(invocation -> {
                     var violation = invocation.<ConnectionLimitViolation>getArgument(0);
                     if (!storedViolations.contains(violation)) {
                         storedViolations.add(violation);
                     }
                     return violation;
                 });
        lenient().when(connectionLimitViolationRepository.findByEndedAtIsNull())
                 .thenAnswer(invocation -> storedViolations.stream().filter(v -> v.endedAt() == null).toList());

        service = serviceWithRecoveryHold(Duration.ZERO);
    }

    private ConnectionLimitViolationDetectionService serviceWithRecoveryHold(Duration recoveryHold) {
        return new ConnectionLimitViolationDetectionService(aiidaRecordRepository,
                                                            connectionLimitViolationRepository,
                                                            permissionRepository,
                                                            connectionLimitService,
                                                            notificationService,
                                                            Clock.fixed(CLOCK_INSTANT, ZoneOffset.UTC),
                                                            recoveryHold.toMillis());
    }

    @Test
    void givenNoAssignment_doesNotNotify() {
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNullAndStatusIn(PermissionStatus.ACTIVE))
                .thenReturn(List.of());

        service.checkViolations();

        verifyNoInteractions(notificationService);
    }

    @Test
    void givenFirstSweep_baselinesToLatestRecordAndSkipsHistory() {
        givenAssignment();
        givenLatestRecordId(10);
        givenNewRecords(10);

        service.checkViolations();

        verify(aiidaRecordRepository).findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, 10L);
        verifyNoInteractions(notificationService);
    }

    @Test
    void givenViolation_storesViolationAndNotifies() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, imported("9.0")));

        service.checkViolations();

        assertEquals(1, storedViolations.size());
        var violation = storedViolations.getFirst();
        assertEquals(PERMISSION_ID, violation.permissionId());
        assertEquals(DATA_SOURCE_ID, violation.dataSourceId());
        assertEquals(TIMESTAMP, violation.startedAt());
        assertEquals(0, BigDecimal.valueOf(9).compareTo(violation.startPowerKw()));
        verify(notificationService).notifyViolationStarted(permission, violation);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void givenViolationThenBackWithinLimits_endsViolationAndNotifies() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, imported("9.0")), aiidaRecord(7, imported("5.0")));

        service.checkViolations();

        var violation = storedViolations.getFirst();
        assertEquals(TIMESTAMP, violation.endedAt());
        var inOrder = inOrder(notificationService);
        inOrder.verify(notificationService).notifyViolationStarted(permission, violation);
        inOrder.verify(notificationService)
               .notifyViolationEnded(eq(permission),
                                     eq(violation),
                                     argThat(power -> power.compareTo(BigDecimal.valueOf(5)) == 0));
    }

    @Test
    void givenRepeatedViolations_storesEachViolationAndNotifies() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6, imported("9.0")),
                        aiidaRecord(7, imported("5.0")),
                        aiidaRecord(8, imported("2.0")));

        service.checkViolations();

        verify(notificationService, times(2)).notifyViolationStarted(eq(permission), any());
        verify(notificationService).notifyViolationEnded(eq(permission), any(), any());
        verify(connectionLimitService, times(1)).getConnectionLimits(any(), any(), any(), any(), any());
        assertEquals(2, storedViolations.size());
        assertEquals(TIMESTAMP, storedViolations.get(0).startedAt());
        assertEquals(TIMESTAMP, storedViolations.get(0).endedAt());
        assertNull(storedViolations.get(1).endedAt());
    }

    @Test
    void givenAssignmentRemovedWhileViolated_notifiesAgainAfterReassignment() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, imported("9.0")));

        service.checkViolations();
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNullAndStatusIn(PermissionStatus.ACTIVE))
                .thenReturn(List.of());
        service.checkViolations();
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNullAndStatusIn(PermissionStatus.ACTIVE))
                .thenReturn(List.of(permission));
        service.checkViolations();

        verify(notificationService, times(2)).notifyViolationStarted(eq(permission), any());
        assertEquals(2, storedViolations.size());
        assertEquals(CLOCK_INSTANT, storedViolations.getFirst().endedAt());
    }

    @Test
    void givenPermissionNoLongerActive_endsViolationWithoutNotification() {
        storedViolations.add(new ConnectionLimitViolation(PERMISSION_ID,
                                                          DATA_SOURCE_ID,
                                                          TIMESTAMP.minusSeconds(60),
                                                          null,
                                                          BigDecimal.valueOf(3),
                                                          BigDecimal.valueOf(8),
                                                          BigDecimal.valueOf(9)));
        // Only active permissions are returned, so the permission was revoked or terminated
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNullAndStatusIn(PermissionStatus.ACTIVE))
                .thenReturn(List.of());

        service.checkViolations();

        assertEquals(CLOCK_INSTANT, storedViolations.getFirst().endedAt());
        verifyNoInteractions(notificationService);
        verifyNoInteractions(aiidaRecordRepository);
    }

    @Test
    void givenViolationOngoingBeforeRestart_doesNotNotifyAgain() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
        storedViolations.add(new ConnectionLimitViolation(PERMISSION_ID,
                                                          DATA_SOURCE_ID,
                                                          TIMESTAMP.minusSeconds(60),
                                                          null,
                                                          BigDecimal.valueOf(3),
                                                          BigDecimal.valueOf(8),
                                                          BigDecimal.valueOf(9)));
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, imported("9.5")));

        service.checkViolations();

        verifyNoInteractions(notificationService);
        assertEquals(0, BigDecimal.valueOf(9.5).compareTo(storedViolations.getFirst().peakPowerKw()));
    }

    @Test
    void givenWorseValues_storesPeakAndNotifiesOnce() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6, imported("9.0")),
                        aiidaRecord(7, imported("10.0")),
                        aiidaRecord(8, imported("9.5")));

        service.checkViolations();

        verify(notificationService).notifyViolationStarted(eq(permission), any());
        verifyNoMoreInteractions(notificationService);
        assertEquals(1, storedViolations.size());
        assertEquals(0, BigDecimal.valueOf(9).compareTo(storedViolations.getFirst().startPowerKw()));
        assertEquals(0, BigDecimal.TEN.compareTo(storedViolations.getFirst().peakPowerKw()));
        assertNull(storedViolations.getFirst().endedAt());
    }

    @Test
    void givenPowerInWatt_convertsToKilowatt() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6,
                                    value(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                          "9000",
                                          UnitOfMeasurement.WATT)));

        service.checkViolations();

        assertEquals(0, BigDecimal.valueOf(9).compareTo(storedViolations.getFirst().startPowerKw()));
    }

    @Test
    void givenPowerOscillatingWithinHold_keepsOneViolation() {
        service = serviceWithRecoveryHold(Duration.ofMinutes(1));
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6, TIMESTAMP, imported("9.0")),
                        aiidaRecord(7, TIMESTAMP.plusSeconds(10), imported("5.0")),
                        aiidaRecord(8, TIMESTAMP.plusSeconds(20), imported("9.5")),
                        aiidaRecord(9, TIMESTAMP.plusSeconds(30), imported("5.0")),
                        aiidaRecord(10, TIMESTAMP.plusSeconds(40), imported("10.0")));

        service.checkViolations();

        assertEquals(1, storedViolations.size());
        assertNull(storedViolations.getFirst().endedAt());
        assertEquals(0, BigDecimal.TEN.compareTo(storedViolations.getFirst().peakPowerKw()));
        verify(notificationService).notifyViolationStarted(eq(permission), any());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void givenPowerWithinLimitsLongerThanHold_endsViolationAtFirstRecoveredRecord() {
        service = serviceWithRecoveryHold(Duration.ofMinutes(1));
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6, TIMESTAMP, imported("9.0")),
                        aiidaRecord(7, TIMESTAMP.plusSeconds(10), imported("5.0")),
                        aiidaRecord(8, TIMESTAMP.plusSeconds(40), imported("4.0")),
                        aiidaRecord(9, TIMESTAMP.plusSeconds(70), imported("4.5")));

        service.checkViolations();

        var violation = storedViolations.getFirst();
        assertEquals(TIMESTAMP.plusSeconds(10), violation.endedAt());
        verify(notificationService).notifyViolationEnded(eq(permission),
                                                         eq(violation),
                                                         argThat(power -> power.compareTo(BigDecimal.valueOf(5)) == 0));
    }

    @Test
    void givenRecoveryPendingAcrossSweeps_endsViolationOnceHoldPassed() {
        service = serviceWithRecoveryHold(Duration.ofMinutes(1));
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6, TIMESTAMP, imported("9.0")),
                        aiidaRecord(7, TIMESTAMP.plusSeconds(10), imported("5.0")));
        givenNewRecords(7, aiidaRecord(8, TIMESTAMP.plusSeconds(30), imported("5.0")));
        givenNewRecords(8, aiidaRecord(9, TIMESTAMP.plusSeconds(70), imported("5.0")));

        service.checkViolations();
        service.checkViolations();
        verify(notificationService, never()).notifyViolationEnded(any(), any(), any());
        service.checkViolations();

        assertEquals(TIMESTAMP.plusSeconds(10), storedViolations.getFirst().endedAt());
        verify(notificationService).notifyViolationEnded(eq(permission), any(), any());
    }

    @Test
    void givenNoFurtherRecordAfterRecovery_keepsViolationOpen() {
        service = serviceWithRecoveryHold(Duration.ofMinutes(1));
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5,
                        aiidaRecord(6, TIMESTAMP, imported("9.0")),
                        aiidaRecord(7, TIMESTAMP.plusSeconds(10), imported("5.0")));
        givenNewRecords(7);

        service.checkViolations();
        service.checkViolations();

        assertNull(storedViolations.getFirst().endedAt());
        verify(notificationService, never()).notifyViolationEnded(any(), any(), any());
    }

    @Test
    void givenRecordsAlreadyChecked_doesNotCheckThemAgain() {
        givenViolationContext();
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, imported("9.0")));
        givenNewRecords(6);

        service.checkViolations();
        service.checkViolations();

        verify(notificationService).notifyViolationStarted(eq(permission), any());
        verify(aiidaRecordRepository).findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, 6L);
    }

    @Test
    void givenNoPowerValue_doesNotNotify() {
        givenAssignment();
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, energyOnly()));

        service.checkViolations();

        verifyNoInteractions(notificationService);
    }

    @Test
    void givenLowerThanMinLimit_storesViolation() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(-5), null);
        givenLatestRecordId(5);
        givenNewRecords(5, aiidaRecord(6, exported("7.0")));

        service.checkViolations();

        assertEquals(0, BigDecimal.valueOf(-7).compareTo(storedViolations.getFirst().startPowerKw()));
    }

    private void givenViolationContext() {
        givenAssignment();
        givenEffectiveLimit(BigDecimal.valueOf(3), BigDecimal.valueOf(8));
    }

    private void givenAssignment() {
        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.monitoringDataSourceId()).thenReturn(DATA_SOURCE_ID);
        when(permissionRepository.findByMonitoringDataSourceIdIsNotNullAndStatusIn(PermissionStatus.ACTIVE))
                .thenReturn(List.of(permission));
    }

    private void givenLatestRecordId(long id) {
        var latest = mock(AiidaRecord.class);
        when(latest.id()).thenReturn(id);
        when(aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(DATA_SOURCE_ID))
                .thenReturn(Optional.of(latest));
    }

    private void givenNewRecords(long afterId, AiidaRecord... records) {
        when(aiidaRecordRepository.findByDataSourceIdAndIdGreaterThanOrderByIdAsc(DATA_SOURCE_ID, afterId))
                .thenReturn(List.of(records));
    }

    private void givenEffectiveLimit(BigDecimal min, BigDecimal max) {
        when(permission.userId()).thenReturn(USER_ID);
        var limit = new ConnectionLimitDto(PERMISSION_ID,
                                           "",
                                           null,
                                           TIMESTAMP.minusSeconds(3600),
                                           TIMESTAMP.plusSeconds(3600),
                                           min,
                                           max);
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any(), any()))
                .thenReturn(List.of(limit));
    }

    private AiidaRecord aiidaRecord(long id, AiidaRecordValue... values) {
        return aiidaRecord(id, TIMESTAMP, values);
    }

    private AiidaRecord aiidaRecord(long id, Instant timestamp, AiidaRecordValue... values) {
        var aiidaRecord = mock(AiidaRecord.class);
        when(aiidaRecord.id()).thenReturn(id);
        when(aiidaRecord.aiidaRecordValues()).thenReturn(List.of(values));
        lenient().when(aiidaRecord.timestamp()).thenReturn(timestamp);
        return aiidaRecord;
    }

    private AiidaRecordValue imported(String value) {
        return value(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER, value);
    }

    private AiidaRecordValue exported(String value) {
        return value(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, value);
    }

    private AiidaRecordValue energyOnly() {
        return value(ObisCode.POSITIVE_ACTIVE_ENERGY, "1");
    }

    private AiidaRecordValue value(ObisCode dataTag, String value) {
        return value(dataTag, value, UnitOfMeasurement.KILO_WATT);
    }

    private AiidaRecordValue value(ObisCode dataTag, String value, UnitOfMeasurement unit) {
        return new AiidaRecordValue(dataTag.toString(), dataTag, value, unit, value, unit);
    }
}
