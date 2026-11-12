// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.monitoring;

import energy.eddie.aiida.dtos.monitoring.MeasurementPointDto;
import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.monitoring.PowerSample;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T10:02:30Z");
    private static final Instant FROM = NOW.minus(Duration.ofDays(1));
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");
    private static final Set<ObisCode> POWER_DATA_TAGS = Set.of(POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                NEGATIVE_ACTIVE_INSTANTANEOUS_POWER);

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AiidaRecordRepository aiidaRecordRepository;
    @Mock
    private AuthService authService;

    private MeasurementService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new MeasurementService(permissionRepository, aiidaRecordRepository, authService);
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void unknownPermissionOrPermissionOfAnotherUser_isNotFound() {
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.getMeasurements(PERMISSION_ID, FROM, NOW));
        verifyNoInteractions(aiidaRecordRepository);
    }

    @Test
    void permissionWithoutConnectionLimits_isNotAllowed() {
        givenPermission(permission(false, DATA_SOURCE_ID));

        assertThrows(ConnectionLimitMonitoringNotAllowedException.class,
                     () -> service.getMeasurements(PERMISSION_ID, FROM, NOW));
        verifyNoInteractions(aiidaRecordRepository);
    }

    @Test
    void withoutMonitoredDataSource_returnsEmptyList() throws Exception {
        givenPermission(permission(true, null));

        assertTrue(service.getMeasurements(PERMISSION_ID, FROM, NOW).isEmpty());
        verifyNoInteractions(aiidaRecordRepository);
    }

    @Test
    void measurementsAreReadFromTheMonitoredDataSource() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of());

        service.getMeasurements(PERMISSION_ID, FROM, NOW);

        verify(aiidaRecordRepository).findPowerSamplesByDataSourceId(DATA_SOURCE_ID, FROM, NOW, POWER_DATA_TAGS);
    }

    @Test
    void importAndExportOfARecord_areReturnedAsNetPower() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of(sample(1L, "2026-07-10T09:55:10Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "5"),
                             sample(1L, "2026-07-10T09:55:10Z", NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, "2")));

        assertSinglePoint("3", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void recordsAreReturnedOrderedByTimestamp() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of(sample(2L, "2026-07-10T09:56:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "2"),
                             sample(1L, "2026-07-10T09:55:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "1")));

        var points = service.getMeasurements(PERMISSION_ID, FROM, NOW);

        assertEquals(List.of(Instant.parse("2026-07-10T09:55:00Z"), Instant.parse("2026-07-10T09:56:00Z")),
                     points.stream().map(MeasurementPointDto::timestamp).toList());
    }

    @Test
    void watt_isConvertedToKilowatt() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of(new PowerSample(1L, Instant.parse("2026-07-10T09:55:00Z"),
                                             POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "4200", UnitOfMeasurement.WATT)));

        assertSinglePoint("4.2", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void malformedValue_isIgnoredWithoutAffectingTheOtherDirection() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of(sample(1L, "2026-07-10T09:55:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "broken"),
                             sample(1L, "2026-07-10T09:55:00Z", NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, "2")));

        assertSinglePoint("-2", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void recordWithoutValidPowerValue_isDropped() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of(sample(1L, "2026-07-10T09:55:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "broken"),
                             sample(2L, "2026-07-10T09:56:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "4")));

        assertSinglePoint("4", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void duplicateDataTag_isSummed() throws Exception {
        givenPermission(permission(true, DATA_SOURCE_ID));
        givenRecords(List.of(sample(1L, "2026-07-10T09:55:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "3"),
                             sample(1L, "2026-07-10T09:55:00Z", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "4")));

        assertSinglePoint("7", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    private void givenPermission(Permission permission) {
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID))
                .thenReturn(Optional.of(permission));
    }

    private static Permission permission(boolean supportsConnectionLimits, @Nullable UUID monitoringDataSourceId) {
        var permission = mock(Permission.class);
        when(permission.supportsConnectionLimits()).thenReturn(supportsConnectionLimits);
        if (supportsConnectionLimits) {
            when(permission.monitoringDataSourceId()).thenReturn(monitoringDataSourceId);
        }
        return permission;
    }

    private void givenRecords(List<PowerSample> samples) {
        when(aiidaRecordRepository.findPowerSamplesByDataSourceId(any(), any(), any(), any())).thenReturn(samples);
    }

    private static PowerSample sample(long recordId, String timestamp, ObisCode dataTag, String value) {
        return new PowerSample(recordId, Instant.parse(timestamp), dataTag, value, UnitOfMeasurement.KILO_WATT);
    }

    private static void assertSinglePoint(String expectedPowerKw, List<MeasurementPointDto> points) {
        assertEquals(1, points.size());
        assertEquals(0, new BigDecimal(expectedPowerKw).compareTo(points.getFirst().powerKw()));
    }
}
