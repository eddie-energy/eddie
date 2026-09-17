// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.monitoring;

import energy.eddie.aiida.dtos.monitoring.MeasurementPointDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotMonitorableException;
import energy.eddie.aiida.models.monitoring.PowerSample;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static energy.eddie.api.agnostic.aiida.ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER;
import static energy.eddie.api.agnostic.aiida.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;

@Service
public class MeasurementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementService.class);
    private static final Set<ObisCode> POWER_DATA_TAGS = Set.of(POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                                                ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER);
    /**
     * Upper bound for the number of returned points. Measurement intervals vary from sub-second to daily, so a
     * high-frequency source would otherwise produce hundreds of thousands of points for a multi-day interval.
     * When the bound is exceeded, points are merged into equally sized time buckets.
     */
    private static final int MAX_POINTS = 1_000;

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

    public List<MeasurementPointDto> getMeasurements(
            UUID permissionId,
            Instant from,
            Instant to
    ) throws InvalidUserException, UnauthorizedException, PermissionNotFoundException,
             PermissionNotMonitorableException {
        var permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        authService.checkAuthorizationForPermission(permission);
        requireMonitorable(permission);

        var userId = permission.userId();
        var meterId = permission.meterId();

        if (userId == null || meterId == null || meterId.isBlank()) {
            LOGGER.debug("Permission {} does not reference a meter, no measurements can be resolved", permissionId);
            return List.of();
        }

        var source = resolveMeasurementSource(permission, userId, meterId);

        if (source == null) {
            return List.of();
        }

        var samples = aiidaRecordRepository.findPowerSamplesByDataSourceId(source.dataSourceId(),
                                                                          from,
                                                                          to,
                                                                          source.powerDataTags());
        return downsample(points(samples));
    }

    private void requireMonitorable(Permission permission) throws PermissionNotMonitorableException {
        var dataNeed = permission.dataNeed();
        var monitorable = permission.status().isActive()
                          && dataNeed != null
                          && InboundAiidaDataNeed.DISCRIMINATOR_VALUE.equals(dataNeed.type())
                          && dataNeed.schemas().contains(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12);

        if (!monitorable) {
            throw new PermissionNotMonitorableException(permission.id());
        }
    }

    private @Nullable MeasurementSource resolveMeasurementSource(Permission permission, UUID userId, String meterId) {
        var outboundPermissions = resolveOutboundPermissions(permission, userId, meterId);

        if (outboundPermissions.isEmpty()) {
            LOGGER.debug("No active outbound permission of user {} measures meter {}", userId, meterId);
            return null;
        }

        if (outboundPermissions.size() > 1) {
            LOGGER.warn("Meter {} of user {} is measured by {} active outbound permissions, measurements are ambiguous",
                        meterId,
                        userId,
                        outboundPermissions.size());
            return null;
        }

        var outboundPermission = outboundPermissions.getFirst();
        var dataSource = outboundPermission.dataSource();

        if (dataSource == null) {
            LOGGER.warn("Outbound permission {} lost its data source while measurements were resolved",
                        outboundPermission.id());
            return null;
        }

        var powerDataTags = allowedPowerDataTags(outboundPermission);

        if (powerDataTags.isEmpty()) {
            LOGGER.debug("Outbound permission {} covers no instantaneous active power", outboundPermission.id());
            return null;
        }

        return new MeasurementSource(dataSource.id(), powerDataTags);
    }

    private List<Permission> resolveOutboundPermissions(Permission permission, UUID userId, String meterId) {
        var dataNeed = permission.dataNeed();
        var isFlexibleConnectionAgreement = dataNeed != null
                                            && dataNeed.contexts()
                                                       .contains(AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT);

        if (isFlexibleConnectionAgreement) {
            var contextual = permissionRepository.findActiveOutboundByMeterIdAndContext(
                    userId,
                    meterId,
                    AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT);

            if (!contextual.isEmpty()) {
                return contextual;
            }
        }

        return permissionRepository.findActiveOutboundByMeterId(userId, meterId);
    }

    private static Set<ObisCode> allowedPowerDataTags(Permission outboundPermission) {
        if (!(outboundPermission.dataNeed() instanceof OutboundAiidaLocalDataNeed dataNeed)) {
            return Set.of();
        }

        var powerDataTags = EnumSet.noneOf(ObisCode.class);
        powerDataTags.addAll(dataNeed.dataTags());
        powerDataTags.retainAll(POWER_DATA_TAGS);
        return powerDataTags;
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
                points.add(new MeasurementPointDto(recordSamples.getFirst().timestamp(), powerKw, powerKw));
            }
        }

        points.sort(Comparator.comparing(MeasurementPointDto::timestamp));
        return points;
    }

    /**
     * Reduces the number of points to at most {@link #MAX_POINTS} by merging them into equally sized time buckets.
     * Each bucket keeps the minimum and maximum power within it, so peaks are preserved while the point count stays
     * bounded regardless of how frequently the source measures.
     */
    private static List<MeasurementPointDto> downsample(List<MeasurementPointDto> points) {
        if (points.size() <= MAX_POINTS) {
            return points;
        }

        var first = points.getFirst().timestamp();
        var last = points.getLast().timestamp();
        var bucketSizeMillis = Math.max(1, Duration.between(first, last).toMillis() / MAX_POINTS);

        var buckets = new LinkedHashMap<Long, MeasurementPointDto>();
        for (var point : points) {
            var offsetMillis = Duration.between(first, point.timestamp()).toMillis();
            var bucketIndex = Math.min(offsetMillis / bucketSizeMillis, MAX_POINTS - 1);
            var existing = buckets.get(bucketIndex);

            if (existing == null) {
                buckets.put(bucketIndex,
                            new MeasurementPointDto(first.plusMillis(bucketIndex * bucketSizeMillis),
                                                    point.minPowerKw(),
                                                    point.maxPowerKw()));
            } else {
                buckets.put(bucketIndex,
                            new MeasurementPointDto(existing.timestamp(),
                                                    existing.minPowerKw().min(point.minPowerKw()),
                                                    existing.maxPowerKw().max(point.maxPowerKw())));
            }
        }

        return new ArrayList<>(buckets.values());
    }

    private static @Nullable BigDecimal netPowerKw(List<PowerSample> recordSamples) {
        var powerKw = BigDecimal.ZERO;

        for (var sample : recordSamples) {
            var value = powerKw(sample);

            if (value == null) {
                return null;
            }

            if (sample.dataTag() == POSITIVE_ACTIVE_INSTANTANEOUS_POWER) {
                powerKw = powerKw.add(value);
            } else if (sample.dataTag() == NEGATIVE_ACTIVE_INSTANTANEOUS_POWER) {
                powerKw = powerKw.subtract(value);
            }
        }

        return recordSamples.isEmpty() ? null : powerKw;
    }

    private static @Nullable BigDecimal powerKw(PowerSample sample) {
        BigDecimal value;
        try {
            value = new BigDecimal(sample.value().trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("Ignoring record {}: value '{}' of {} is not a number",
                        sample.recordId(),
                        sample.value(),
                        sample.dataTag());
            return null;
        }

        return switch (sample.unitOfMeasurement()) {
            case KILO_WATT -> value;
            case WATT -> value.movePointLeft(3);
            default -> {
                LOGGER.warn("Ignoring record {}: unit {} of {} is not a power unit",
                            sample.recordId(),
                            sample.unitOfMeasurement(),
                            sample.dataTag());
                yield null;
            }
        };
    }

    private record MeasurementSource(
            UUID dataSourceId,
            Set<ObisCode> powerDataTags
    ) {
    }
}
