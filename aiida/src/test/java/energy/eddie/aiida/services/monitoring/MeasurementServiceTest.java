// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.monitoring;

import energy.eddie.aiida.dtos.monitoring.MeasurementPointDto;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotMonitorableException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.monitoring.PowerSample;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static energy.eddie.api.agnostic.aiida.ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MeasurementServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T10:02:30Z");
    private static final Instant FROM = NOW.minus(Duration.ofDays(1));
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");
    private static final String METER_ID = "003114735";

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AiidaRecordRepository aiidaRecordRepository;
    @Mock
    private AuthService authService;

    private MeasurementService service;

    @BeforeEach
    void setUp() {
        service = new MeasurementService(permissionRepository, aiidaRecordRepository, authService);
    }

    @Test
    void unknownPermission_isNotFound() {
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void permissionOfAnotherUser_isUnauthorized() throws Exception {
        var permission = monitorablePermission();
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));
        doThrow(new UnauthorizedException("unauthorized")).when(authService).checkAuthorizationForPermission(permission);

        assertThrows(UnauthorizedException.class, () -> service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void inactivePermission_isNotMonitorable() {
        var permission = monitorablePermission();
        when(permission.status()).thenReturn(PermissionStatus.REVOKED);
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));

        assertThrows(PermissionNotMonitorableException.class,
                     () -> service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void permissionWithoutMeterId_returnsEmptyList() throws Exception {
        var permission = monitorablePermission();
        when(permission.meterId()).thenReturn(" ");
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));

        assertTrue(service.getMeasurements(PERMISSION_ID, FROM, NOW).isEmpty());
    }

    @Test
    void ambiguousOutboundPermissions_returnEmptyList() throws Exception {
        givenMonitorablePermission();
        var firstPermission = outboundPermission(DATA_SOURCE_ID, POSITIVE_ACTIVE_INSTANTANEOUS_POWER);
        var secondPermission = outboundPermission(UUID.randomUUID(), POSITIVE_ACTIVE_INSTANTANEOUS_POWER);
        when(permissionRepository.findActiveOutboundByMeterIdAndContext(any(), any(), any()))
                .thenReturn(List.of(firstPermission, secondPermission));

        assertTrue(service.getMeasurements(PERMISSION_ID, FROM, NOW).isEmpty());
        verify(aiidaRecordRepository, never()).findPowerSamplesByDataSourceId(any(), any(), any(), any());
    }

    @Test
    void importAndExport_areReturnedAsNetPower() throws Exception {
        givenSingleOutboundPermission(POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                      NEGATIVE_ACTIVE_INSTANTANEOUS_POWER);
        givenRawRecords(List.of(sample(1L, "2026-07-10T09:55:10Z",
                                       POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "5", UnitOfMeasurement.KILO_WATT),
                                sample(1L, "2026-07-10T09:55:10Z",
                                       NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, "2", UnitOfMeasurement.KILO_WATT)));

        assertSinglePoint("3", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void wattValues_areConvertedToKilowatt() throws Exception {
        givenSingleOutboundPermission(POSITIVE_ACTIVE_INSTANTANEOUS_POWER);
        givenRawRecords(List.of(sample(1L, "2026-07-10T09:55:00Z",
                                       POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "4200", UnitOfMeasurement.WATT)));

        assertSinglePoint("4.2", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void malformedValue_dropsOnlyTheAffectedRecord() throws Exception {
        givenSingleOutboundPermission(POSITIVE_ACTIVE_INSTANTANEOUS_POWER);
        givenRawRecords(List.of(sample(1L, "2026-07-10T09:55:00Z",
                                       POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "broken", UnitOfMeasurement.KILO_WATT),
                                sample(2L, "2026-07-10T09:56:00Z",
                                       POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "4", UnitOfMeasurement.KILO_WATT)));

        assertSinglePoint("4", service.getMeasurements(PERMISSION_ID, FROM, NOW));
    }

    @Test
    void highFrequencyMeasurements_areDownsampledKeepingMinAndMax() throws Exception {
        givenSingleOutboundPermission(POSITIVE_ACTIVE_INSTANTANEOUS_POWER);

        var samples = new ArrayList<PowerSample>();
        for (var i = 0; i < 2_500; i++) {
            samples.add(sample(i + 1L,
                               NOW.minusSeconds(i).toString(),
                               POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                               String.valueOf(i),
                               UnitOfMeasurement.KILO_WATT));
        }
        givenRawRecords(samples);

        var points = service.getMeasurements(PERMISSION_ID, FROM, NOW);

        assertEquals(1_000, points.size());
        assertEquals(0, new BigDecimal("2499").compareTo(points.getFirst().maxPowerKw()));
        assertEquals(0, new BigDecimal("0").compareTo(points.getLast().minPowerKw()));
    }

    private void givenSingleOutboundPermission(ObisCode... dataTags) {
        givenMonitorablePermission();
        var permission = outboundPermission(DATA_SOURCE_ID, dataTags);
        when(permissionRepository.findActiveOutboundByMeterIdAndContext(any(), any(), any()))
                .thenReturn(List.of(permission));
    }

    private void givenMonitorablePermission() {
        var permission = monitorablePermission();
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(permission));
    }

    private static Permission monitorablePermission() {
        var dataNeed = mock(InboundAiidaLocalDataNeed.class);
        when(dataNeed.type()).thenReturn(InboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        when(dataNeed.schemas()).thenReturn(Set.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12));
        when(dataNeed.contexts()).thenReturn(Set.of(AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT));

        var permission = mock(Permission.class);
        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.userId()).thenReturn(USER_ID);
        when(permission.meterId()).thenReturn(METER_ID);
        when(permission.status()).thenReturn(PermissionStatus.STREAMING_DATA);
        when(permission.dataNeed()).thenReturn(dataNeed);
        return permission;
    }

    private static Permission outboundPermission(UUID dataSourceId, ObisCode... dataTags) {
        var dataSource = mock(DataSource.class);
        when(dataSource.id()).thenReturn(dataSourceId);

        var dataNeed = mock(OutboundAiidaLocalDataNeed.class);
        when(dataNeed.dataTags()).thenReturn(Set.of(dataTags));

        var permission = mock(Permission.class);
        when(permission.id()).thenReturn(UUID.randomUUID());
        when(permission.dataSource()).thenReturn(dataSource);
        when(permission.dataNeed()).thenReturn(dataNeed);
        return permission;
    }

    private void givenRawRecords(List<PowerSample> samples) {
        when(aiidaRecordRepository.findPowerSamplesByDataSourceId(eq(DATA_SOURCE_ID), any(), any(), any()))
                .thenReturn(samples);
    }

    private static PowerSample sample(
            long recordId,
            String timestamp,
            ObisCode dataTag,
            String value,
            UnitOfMeasurement unit
    ) {
        return new PowerSample(recordId, Instant.parse(timestamp), dataTag, value, unit);
    }

    private static void assertSinglePoint(String expectedPowerKw, List<MeasurementPointDto> points) {
        assertEquals(1, points.size());
        assertEquals(0, new BigDecimal(expectedPowerKw).compareTo(points.getFirst().minPowerKw()));
        assertEquals(0, new BigDecimal(expectedPowerKw).compareTo(points.getFirst().maxPowerKw()));
    }
}
