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
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Periodically checks the latest measured data of assigned data sources against the effective connection limits of the
 * permissions and persists the resulting violations. The users are notified when a violation starts and when it ends.
 * <p>
 * The check interval can be configured with {@code aiida.connection-limit.violation-detection.interval-ms} (default: 10 seconds).
 * Detection can be disabled with {@code aiida.connection-limit.violation-detection.enabled=false}.
 * <p>
 * A violation only ends once the measured power stayed within the limits for the recovery hold
 * ({@code aiida.connection-limit.violation-detection.recovery-hold-ms}, default: 1 minute),
 * so that a value oscillating around a limit is a single violation and does not flood the user with notifications.
 * The violation ends at the first record that was back within the limits.
 * Only permissions that are active are monitored, the violations of other permissions end.
 * A record without an effective limit counts as within the limits, as no limits means no violation.
 * The hold is measured in record timestamps, so a violation stays open until a record confirms the recovery.
 */
@Service
@ConditionalOnProperty(prefix = "aiida.connection-limit.violation-detection", name = "enabled", matchIfMissing = true)
public class ConnectionLimitViolationDetectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitViolationDetectionService.class);

    private final AiidaRecordRepository aiidaRecordRepository;
    private final ConnectionLimitViolationRepository connectionLimitViolationRepository;
    private final PermissionRepository permissionRepository;
    private final ConnectionLimitService connectionLimitService;
    private final ConnectionLimitNotificationService connectionLimitNotificationService;
    private final Clock clock;
    private final long recoveryHoldMs;
    private final Map<UUID, PendingRecovery> pendingRecoveries = new HashMap<>();
    private final Map<UUID, Long> lastCheckedRecordId = new HashMap<>();

    public ConnectionLimitViolationDetectionService(
            AiidaRecordRepository aiidaRecordRepository,
            ConnectionLimitViolationRepository connectionLimitViolationRepository,
            PermissionRepository permissionRepository,
            ConnectionLimitService connectionLimitService,
            ConnectionLimitNotificationService connectionLimitNotificationService,
            Clock clock,
            @Value("${aiida.connection-limit.violation-detection.recovery-hold-ms:60000}") long recoveryHoldMs
    ) {
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.connectionLimitViolationRepository = connectionLimitViolationRepository;
        this.permissionRepository = permissionRepository;
        this.connectionLimitService = connectionLimitService;
        this.connectionLimitNotificationService = connectionLimitNotificationService;
        this.clock = clock;
        this.recoveryHoldMs = recoveryHoldMs;
    }

    @Scheduled(fixedDelayString = "${aiida.connection-limit.violation-detection.interval-ms:10000}")
    void checkViolations() {
        var permissionsByDataSource = permissionRepository.findByMonitoringDataSourceIdIsNotNullAndStatusIn(
                                                                  PermissionStatus.ACTIVE)
                                                          .stream()
                                                          .collect(Collectors.groupingBy(permission -> Objects.requireNonNull(
                                                                  permission.monitoringDataSourceId())));
        forgetUnassigned(permissionsByDataSource);
        var openViolations = loadOpenViolations(permissionsByDataSource);

        for (var dataSource : permissionsByDataSource.entrySet()) {
            try {
                check(dataSource.getKey(), dataSource.getValue(), openViolations);
            } catch (Exception e) {
                LOGGER.error("Failed to check connection limits for data source {}", dataSource.getKey(), e);
            }
        }
    }

    /**
     * Drops the record watermark of data sources that are no longer monitored, so that a later assignment starts from a
     * clean state.
     */
    private void forgetUnassigned(Map<UUID, List<Permission>> permissionsByDataSource) {
        lastCheckedRecordId.keySet().retainAll(permissionsByDataSource.keySet());
    }

    /**
     * Loads the ongoing violations and ends those of permissions that are no longer monitored.
     *
     * @return the ongoing violations by permission ID.
     */
    private Map<UUID, ConnectionLimitViolation> loadOpenViolations(Map<UUID, List<Permission>> permissionsByDataSource) {
        var monitoredPermissionIds = permissionsByDataSource.values()
                                                            .stream()
                                                            .flatMap(List::stream)
                                                            .map(Permission::id)
                                                            .collect(Collectors.toSet());
        var openViolations = new HashMap<UUID, ConnectionLimitViolation>();
        for (var violation : connectionLimitViolationRepository.findByEndedAtIsNull()) {
            if (monitoredPermissionIds.contains(violation.permissionId())) {
                openViolations.put(violation.permissionId(), violation);
            } else {
                violation.end(clock.instant());
                connectionLimitViolationRepository.save(violation);
            }
        }
        pendingRecoveries.keySet().retainAll(openViolations.keySet());
        return openViolations;
    }

    private void check(
            UUID dataSourceId,
            List<Permission> permissions,
            Map<UUID, ConnectionLimitViolation> openViolations
    ) {
        var lastCheckedId = lastCheckedRecordId.computeIfAbsent(dataSourceId, this::latestRecordId);
        var records = aiidaRecordRepository.findByDataSourceIdAndIdGreaterThanOrderByIdAsc(dataSourceId, lastCheckedId);
        if (records.isEmpty()) {
            return;
        }

        // Limits rarely change, so they are queried once per permission for all new records instead of once per record.
        // Records are ordered by id, which is chronological.
        var from = records.getFirst().timestamp();
        var to = records.getLast().timestamp().plusMillis(1);
        var limitsByPermission = new HashMap<UUID, List<ConnectionLimitDto>>();

        for (var aiidaRecord : records) {
            var netPower = netPower(aiidaRecord);
            if (netPower != null) {
                for (var permission : permissions) {
                    var limits = limitsByPermission.computeIfAbsent(permission.id(),
                                                                    id -> limits(permission, from, to));
                    evaluate(permission, dataSourceId, limits, netPower, aiidaRecord.timestamp(), openViolations);
                }
            }

            lastCheckedRecordId.put(dataSourceId, aiidaRecord.id());
        }
    }

    /**
     * Returns the id of the latest record of the data source, used to initialize the watermark so that pre-existing
     * history is not replayed when a data source is first seen.
     */
    private long latestRecordId(UUID dataSourceId) {
        return aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(dataSourceId).map(AiidaRecord::id).orElse(0L);
    }

    private void evaluate(
            Permission permission,
            UUID dataSourceId,
            List<ConnectionLimitDto> limits,
            BigDecimal netPower, Instant timestamp, Map<UUID, ConnectionLimitViolation> openViolations
    ) {
        var effectiveLimit = effectiveLimit(limits, timestamp);
        var violatedLimit = effectiveLimit != null && isViolated(netPower, effectiveLimit) ? effectiveLimit : null;
        var openViolation = openViolations.get(permission.id());

        if (violatedLimit != null) {
            pendingRecoveries.remove(permission.id());
            if (openViolation == null) {
                var violation = new ConnectionLimitViolation(permission.id(),
                                                             dataSourceId,
                                                             timestamp,
                                                             violatedLimit.documentId(),
                                                             violatedLimit.minKw(),
                                                             violatedLimit.maxKw(),
                                                             netPower);
                var savedViolation = connectionLimitViolationRepository.save(violation);
                openViolations.put(permission.id(), savedViolation);
                connectionLimitNotificationService.notifyViolationStarted(permission, savedViolation);
            } else if (openViolation.registerPower(netPower)) {
                connectionLimitViolationRepository.save(openViolation);
            }
        } else if (openViolation != null) {
            var recovery = pendingRecoveries.computeIfAbsent(permission.id(),
                                                             id -> new PendingRecovery(timestamp, netPower));
            if (!timestamp.isBefore(recovery.since().plusMillis(recoveryHoldMs))) {
                openViolation.end(recovery.since());
                connectionLimitViolationRepository.save(openViolation);
                openViolations.remove(permission.id());
                pendingRecoveries.remove(permission.id());
                connectionLimitNotificationService.notifyViolationEnded(permission, openViolation, recovery.powerKw());
            }
        }
    }

    /**
     * Returns the measured net power of a record, which is the imported minus the exported instantaneous power, or
     * null if the record contains neither.
     */
    private @Nullable BigDecimal netPower(AiidaRecord aiidaRecord) {
        var values = aiidaRecord.aiidaRecordValues();
        var imported = powerValue(values, ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER);
        var exported = powerValue(values, ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER);
        if (imported == null && exported == null) {
            return null;
        }

        return Objects.requireNonNullElse(imported, BigDecimal.ZERO)
                      .subtract(Objects.requireNonNullElse(exported, BigDecimal.ZERO));
    }

    private @Nullable BigDecimal powerValue(List<AiidaRecordValue> values, ObisCode dataTag) {
        var value = values.stream().filter(recordValue -> recordValue.dataTag() == dataTag).findFirst().orElse(null);
        if (value == null) {
            return null;
        }

        try {
            var power = new BigDecimal(value.value());
            return switch (value.unitOfMeasurement()) {
                case KILO_WATT -> power;
                case WATT -> power.movePointLeft(3);
                default -> {
                    LOGGER.warn("Ignoring power value of data tag {}: unit {} is not a power unit",
                                dataTag,
                                value.unitOfMeasurement());
                    yield null;
                }
            };
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse power value '{}' for data tag {}", value.value(), dataTag);
            return null;
        }
    }

    private List<ConnectionLimitDto> limits(Permission permission, Instant from, Instant to) {
        var userId = permission.userId();
        if (userId == null) {
            return List.of();
        }

        var meterId = permission.meterId() == null ? "" : permission.meterId();
        return connectionLimitService.getConnectionLimits(userId, permission.id(), meterId, from, to)
                                     .stream()
                                     .filter(limit -> Objects.equals(meterId, limit.meterId()))
                                     .toList();
    }

    private @Nullable EffectiveLimit effectiveLimit(List<ConnectionLimitDto> limits, Instant timestamp) {
        return limits.stream()
                     .filter(limit -> !timestamp.isBefore(limit.intervalStart()) && timestamp.isBefore(limit.intervalEnd()))
                     .findFirst()
                     .map(limit -> new EffectiveLimit(limit.documentId(), limit.minLimitKw(), limit.maxLimitKw()))
                     .orElse(null);
    }

    private boolean isViolated(BigDecimal netPower, EffectiveLimit limit) {
        return (limit.maxKw() != null && netPower.compareTo(limit.maxKw()) > 0) || (limit.minKw() != null && netPower.compareTo(
                limit.minKw()) < 0);
    }

    /**
     * The first record that was back within the limits of an open violation.
     */
    private record PendingRecovery(Instant since, BigDecimal powerKw) {}

    private record EffectiveLimit(@Nullable String documentId, @Nullable BigDecimal minKw,
                                  @Nullable BigDecimal maxKw) {}
}
