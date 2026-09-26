// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitMonitoring;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.ConnectionLimitMonitoringRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.UserSettingsService;
import energy.eddie.api.agnostic.aiida.ObisCode;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ConnectionLimitMonitoringRepository connectionLimitMonitoringRepository;
    private final AiidaRecordRepository aiidaRecordRepository;
    private final PermissionRepository permissionRepository;
    private final ConnectionLimitService connectionLimitService;
    private final UserSettingsService userSettingsService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String sender;
    private final Map<UUID, Boolean> violatedPermissions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCheckedRecordId = new ConcurrentHashMap<>();

    public ConnectionLimitNotificationService(
            ConnectionLimitMonitoringRepository connectionLimitMonitoringRepository,
            AiidaRecordRepository aiidaRecordRepository,
            PermissionRepository permissionRepository,
            ConnectionLimitService connectionLimitService,
            UserSettingsService userSettingsService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String sender
    ) {
        this.connectionLimitMonitoringRepository = connectionLimitMonitoringRepository;
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.permissionRepository = permissionRepository;
        this.connectionLimitService = connectionLimitService;
        this.userSettingsService = userSettingsService;
        this.mailSenderProvider = mailSenderProvider;
        this.sender = sender.isBlank() ? DEFAULT_SENDER : sender;
    }

    @Scheduled(fixedDelayString = "${aiida.notification.interval-ms:10000}")
    void checkViolations() {
        var permissionIdsByDataSource = permissionIdsByDataSource(connectionLimitMonitoringRepository.findAll());
        for (var dataSource : permissionIdsByDataSource.entrySet()) {
            try {
                check(dataSource.getKey(), dataSource.getValue());
            } catch (Exception e) {
                LOGGER.error("Failed to check connection limits for data source {}", dataSource.getKey(), e);
            }
        }
    }

    private Map<UUID, List<UUID>> permissionIdsByDataSource(List<ConnectionLimitMonitoring> assignments) {
        return assignments.stream()
                          .collect(Collectors.groupingBy(
                                  ConnectionLimitMonitoring::dataSourceId,
                                  Collectors.mapping(ConnectionLimitMonitoring::permissionId, Collectors.toList())));
    }

    private void check(UUID dataSourceId, List<UUID> permissionIds) {
        var lastCheckedId = lastCheckedRecordId.computeIfAbsent(dataSourceId, this::latestRecordId);
        var records = aiidaRecordRepository.findByDataSourceIdAndIdGreaterThanOrderByIdAsc(dataSourceId, lastCheckedId);
        for (var aiidaRecord : records) {
            var netPower = netPower(aiidaRecord);
            if (netPower != null) {
                for (var permissionId : permissionIds) {
                    evaluate(permissionId, netPower, aiidaRecord.timestamp());
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

    private void evaluate(UUID permissionId, BigDecimal netPower, Instant timestamp) {
        var permission = permissionRepository.findById(permissionId).orElse(null);
        if (permission == null) {
            violatedPermissions.remove(permissionId);
            return;
        }

        var effectiveLimit = effectiveLimit(permission, timestamp);
        var violated = effectiveLimit != null && isViolated(netPower, effectiveLimit);
        var wasViolated = violatedPermissions.getOrDefault(permissionId, false);

        if (violated && !wasViolated) {
            notify(permission, netPower, effectiveLimit, timestamp, true);
            violatedPermissions.put(permissionId, true);
        } else if (!violated && wasViolated) {
            notify(permission, netPower, effectiveLimit, timestamp, false);
            violatedPermissions.put(permissionId, false);
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

    private @Nullable EffectiveLimit effectiveLimit(Permission permission, Instant timestamp) {
        var userId = permission.userId();
        if (userId == null) {
            return null;
        }

        var meterId = permission.meterId() == null ? "" : permission.meterId();
        // The calculation operates on intervals, so query a minimal window around the measurement instant.
        var limits = connectionLimitService.getConnectionLimits(userId,
                                                                permission.id(),
                                                                meterId,
                                                                timestamp,
                                                                timestamp.plusMillis(1));

        return limits.stream()
                     .filter(limit -> Objects.equals(meterId, limit.meterId()))
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

        mailSender.send(message);
        LOGGER.info("Sent {} notification for permission {} to {}",
                    violation ? "violation" : "recovery",
                    permission.id(),
                    recipient);
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
