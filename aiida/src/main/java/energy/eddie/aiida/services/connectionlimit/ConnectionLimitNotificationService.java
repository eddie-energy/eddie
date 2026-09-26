// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.UserSettingsService;
import energy.eddie.api.agnostic.aiida.ObisCode;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Periodically checks the latest measured data of assigned data sources and notifies users by email when the effective
 * connection limits of a permission are violated, and again when the data is back within the limits.
 * <p>
 * The check interval can be configured with {@code aiida.notification.interval-ms} (default: 10 seconds).
 * Notifications are only sent when a mail server is configured ({@code spring.mail.host}) and the user has set a
 * contact email. Both conditions are evaluated per check and can change at any time.
 */
@Service
public class ConnectionLimitNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitNotificationService.class);
    private static final String DEFAULT_SENDER = "no-reply@aiida";

    private final AiidaRecordRepository aiidaRecordRepository;
    private final PermissionRepository permissionRepository;
    private final ConnectionLimitService connectionLimitService;
    private final UserSettingsService userSettingsService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String sender;
    private final Set<UUID> violatedPermissions = new HashSet<>();
    private final Map<UUID, Long> lastCheckedRecordId = new HashMap<>();

    public ConnectionLimitNotificationService(
            AiidaRecordRepository aiidaRecordRepository,
            PermissionRepository permissionRepository,
            ConnectionLimitService connectionLimitService,
            UserSettingsService userSettingsService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String sender
    ) {
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.permissionRepository = permissionRepository;
        this.connectionLimitService = connectionLimitService;
        this.userSettingsService = userSettingsService;
        this.mailSenderProvider = mailSenderProvider;
        this.sender = sender.isBlank() ? DEFAULT_SENDER : sender;
    }

    @Scheduled(fixedDelayString = "${aiida.notification.interval-ms:10000}")
    void checkViolations() {
        var permissionsByDataSource = permissionRepository.findByMonitoringDataSourceIdIsNotNull()
                                                          .stream()
                                                          .collect(Collectors.groupingBy(
                                                                  permission -> Objects.requireNonNull(permission.monitoringDataSourceId())));
        forgetUnassigned(permissionsByDataSource);

        for (var dataSource : permissionsByDataSource.entrySet()) {
            try {
                check(dataSource.getKey(), dataSource.getValue());
            } catch (Exception e) {
                LOGGER.error("Failed to check connection limits for data source {}", dataSource.getKey(), e);
            }
        }
    }

    /**
     * Drops state of data sources and permissions that are no longer monitored, so that a later assignment starts from a
     * clean state.
     */
    private void forgetUnassigned(Map<UUID, List<Permission>> permissionsByDataSource) {
        lastCheckedRecordId.keySet().retainAll(permissionsByDataSource.keySet());
        var monitoredPermissionIds = permissionsByDataSource.values()
                                                            .stream()
                                                            .flatMap(List::stream)
                                                            .map(Permission::id)
                                                            .collect(Collectors.toSet());
        violatedPermissions.retainAll(monitoredPermissionIds);
    }

    private void check(UUID dataSourceId, List<Permission> permissions) {
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
                    evaluate(permission, limits, netPower, aiidaRecord.timestamp());
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
        return aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(dataSourceId)
                                    .map(AiidaRecord::id)
                                    .orElse(0L);
    }

    private void evaluate(
            Permission permission,
            List<ConnectionLimitDto> limits,
            BigDecimal netPower,
            Instant timestamp
    ) {
        var effectiveLimit = effectiveLimit(limits, timestamp);
        var violated = effectiveLimit != null && isViolated(netPower, effectiveLimit);
        var wasViolated = violatedPermissions.contains(permission.id());

        if (violated && !wasViolated) {
            violatedPermissions.add(permission.id());
            notify(permission, netPower, effectiveLimit, timestamp, true);
        } else if (!violated && wasViolated) {
            violatedPermissions.remove(permission.id());
            notify(permission, netPower, effectiveLimit, timestamp, false);
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
        var value = values.stream()
                          .filter(recordValue -> recordValue.dataTag() == dataTag)
                          .findFirst()
                          .orElse(null);
        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(value.value());
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
                     .map(limit -> new EffectiveLimit(limit.minLimitKw(), limit.maxLimitKw()))
                     .orElse(null);
    }

    private boolean isViolated(BigDecimal netPower, EffectiveLimit limit) {
        return (limit.maxKw() != null && netPower.compareTo(limit.maxKw()) > 0)
               || (limit.minKw() != null && netPower.compareTo(limit.minKw()) < 0);
    }

    private void notify(
            Permission permission,
            BigDecimal netPower,
            @Nullable EffectiveLimit limit,
            Instant timestamp,
            boolean violation
    ) {
        var userId = permission.userId();
        if (userId == null) {
            return;
        }

        var recipient = userSettingsService.findContactEmail(userId).orElse(null);
        if (recipient == null) {
            LOGGER.debug("No contact email configured for user {}; skipping notification for permission {}",
                         userId,
                         permission.id());
            return;
        }

        var mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.debug("No mail server configured; skipping notification for permission {}", permission.id());
            return;
        }

        var message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(violation ? "Connection limits exceeded" : "Connection limits restored");
        message.setText(buildMessage(permission, netPower, limit, timestamp, violation));

        var type = violation ? "violation" : "recovery";
        try {
            mailSender.send(message);
            LOGGER.info("Sent {} notification for permission {} to user {}", type, permission.id(), userId);
        } catch (MailException e) {
            // Not retried, as a failing mail server must not block checking further records
            LOGGER.warn("Failed to send {} notification for permission {} to user {}", type, permission.id(), userId, e);
        }
    }

    private String buildMessage(
            Permission permission,
            BigDecimal netPower,
            @Nullable EffectiveLimit limit,
            Instant timestamp,
            boolean violation
    ) {
        var name = permission.displayName() != null ? permission.displayName() : permission.serviceName();
        var introduction = violation
                ? "The measured power for \"%s\" exceeded the connection limits.".formatted(name)
                : "The measured power for \"%s\" is back within the connection limits.".formatted(name);

        return """
                %s

                Time: %s
                Measured power: %s kW
                Allowed limits: %s
                """.formatted(introduction, timestamp, format(netPower), formatLimits(limit));
    }

    private String formatLimits(@Nullable EffectiveLimit limit) {
        if (limit == null) {
            return "none";
        }

        var min = limit.minKw();
        var max = limit.maxKw();
        if (min == null) {
            return max == null ? "none" : "max %s kW".formatted(format(max));
        }
        if (max == null) {
            return "min %s kW".formatted(format(min));
        }
        return "min %s kW, max %s kW".formatted(format(min), format(max));
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private record EffectiveLimit(@Nullable BigDecimal minKw, @Nullable BigDecimal maxKw) {}
}
