// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.monitoring;

import energy.eddie.aiida.dtos.monitoring.MeasurementPointDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.monitoring.PowerSample;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Provides the measured power of the data source that is monitored for the connection limits of a permission.
 * Each retained record yields one point.
 */
@Service
public class MeasurementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementService.class);
    private static final Set<ObisCode> POWER_DATA_TAGS = Set.of(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER);

    private final PermissionRepository permissionRepository;
    private final AiidaRecordRepository aiidaRecordRepository;
    private final AuthService authService;

    public MeasurementService(
            PermissionRepository permissionRepository,
            AiidaRecordRepository aiidaRecordRepository,
            AuthService authService
    ) {
        this.permissionRepository = permissionRepository;
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.authService = authService;
    }

    /**
     * Returns the signed power of the monitored data source of the permission, ordered by timestamp.
     * The list is empty if no data source is monitored or no records are retained.
     */
    public List<MeasurementPointDto> getMeasurements(
            UUID permissionId,
            Instant from,
            Instant to
    ) throws InvalidUserException, PermissionNotFoundException, ConnectionLimitMonitoringNotAllowedException {
        var permission = permissionRepository.findByPermissionIdAndUserId(permissionId,
                                                                          authService.getCurrentUserId())
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        if (!permission.supportsConnectionLimits()) {
            throw new ConnectionLimitMonitoringNotAllowedException(permissionId);
        }

        var dataSourceId = permission.monitoringDataSourceId();
        if (dataSourceId == null) {
            return List.of();
        }

        var samples = aiidaRecordRepository.findPowerSamplesByDataSourceId(dataSourceId, from, to, POWER_DATA_TAGS);
        return points(samples);
    }

    private static List<MeasurementPointDto> points(List<PowerSample> samples) {
        var samplesByRecord = new LinkedHashMap<Long, List<PowerSample>>();
        for (var sample : samples) {
            samplesByRecord.computeIfAbsent(sample.recordId(), ignored -> new ArrayList<>()).add(sample);
        }

        var points = new ArrayList<MeasurementPointDto>(samplesByRecord.size());
        for (var recordSamples : samplesByRecord.values()) {
            var powerKw = netPowerKw(recordSamples);
            if (powerKw != null) {
                points.add(new MeasurementPointDto(recordSamples.getFirst().timestamp(), powerKw));
            }
        }
        points.sort(Comparator.comparing(MeasurementPointDto::timestamp));
        return points;
    }

    /**
     * Imported minus exported power of a record. Invalid values are ignored individually and values of the same tag
     * are summed. Returns null if the record has no valid power value.
     */
    private static @Nullable BigDecimal netPowerKw(List<PowerSample> recordSamples) {
        BigDecimal imported = null;
        BigDecimal exported = null;

        for (var sample : recordSamples) {
            var powerKw = powerKw(sample);
            if (powerKw == null) {
                continue;
            }
            if (sample.dataTag() == ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER) {
                imported = imported == null ? powerKw : imported.add(powerKw);
            } else {
                exported = exported == null ? powerKw : exported.add(powerKw);
            }
        }

        if (imported == null && exported == null) {
            return null;
        }
        return Objects.requireNonNullElse(imported, BigDecimal.ZERO)
                      .subtract(Objects.requireNonNullElse(exported, BigDecimal.ZERO));
    }

    private static @Nullable BigDecimal powerKw(PowerSample sample) {
        BigDecimal value;
        try {
            value = new BigDecimal(sample.value().trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Ignoring value '{}' of {} in record {}: not a number",
                        sample.value(),
                        sample.dataTag(),
                        sample.recordId());
            return null;
        }

        if (sample.unitOfMeasurement() == UnitOfMeasurement.KILO_WATT) {
            return value;
        }
        if (sample.unitOfMeasurement() == UnitOfMeasurement.WATT) {
            return value.movePointLeft(3);
        }
        LOGGER.warn("Ignoring value of {} in record {}: unit {} is not a power unit",
                    sample.dataTag(),
                    sample.recordId(),
                    sample.unitOfMeasurement());
        return null;
    }
}
