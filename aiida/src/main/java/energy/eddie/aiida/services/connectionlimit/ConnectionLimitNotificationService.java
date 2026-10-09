// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.UserSettingsService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Notifies users by email when a connection limit violation starts and when it ends.
 * <p>
 * Notifications are only sent when a mail server is configured ({@code spring.mail.host}) and the user has set a contact email.
 * Both conditions are evaluated per notification and can change at any time.
 * Failures are logged and never propagated, as they must not affect the detection of violations.
 */
@Service
public class ConnectionLimitNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitNotificationService.class);
    private static final String DEFAULT_SENDER = "no-reply@aiida";

    private final UserSettingsService userSettingsService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String sender;

    public ConnectionLimitNotificationService(
            UserSettingsService userSettingsService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String sender
    ) {
        this.userSettingsService = userSettingsService;
        this.mailSenderProvider = mailSenderProvider;
        this.sender = sender.isBlank() ? DEFAULT_SENDER : sender;
    }

    public void notifyViolationStarted(Permission permission, ConnectionLimitViolation violation) {
        notify(permission, violation, violation.startPowerKw(), violation.startedAt(), true);
    }

    /**
     * Notifies the user that the violation ended.
     *
     * @param recoveredPowerKw the measured power that is back within the limits.
     */
    public void notifyViolationEnded(
            Permission permission,
            ConnectionLimitViolation violation,
            BigDecimal recoveredPowerKw
    ) {
        notify(permission, violation, recoveredPowerKw, violation.endedAt(), false);
    }

    private void notify(
            Permission permission,
            ConnectionLimitViolation violation,
            BigDecimal powerKw,
            @Nullable Instant timestamp,
            boolean started
    ) {
        var type = started ? "violation" : "recovery";
        try {
            var userId = permission.userId();
            if (userId == null) {
                return;
            }

            var recipient = userSettingsService.findContactEmail(userId).orElse(null);
            if (recipient == null) {
                LOGGER.debug("No contact email configured for user {}; skipping {} notification for permission {}",
                             userId,
                             type,
                             permission.id());
                return;
            }

            var mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                LOGGER.debug("No mail server configured; skipping {} notification for permission {}",
                             type,
                             permission.id());
                return;
            }

            var message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(recipient);
            message.setSubject(started ? "Connection limits exceeded" : "Connection limits restored");
            message.setText(buildMessage(permission, violation, powerKw, timestamp, started));

            mailSender.send(message);
            LOGGER.info("Sent {} notification for permission {} to user {}", type, permission.id(), userId);
        } catch (Exception e) {
            LOGGER.warn("Failed to send {} notification for permission {}", type, permission.id(), e);
        }
    }

    private String buildMessage(
            Permission permission, ConnectionLimitViolation violation,
            BigDecimal netPower, @Nullable Instant timestamp, boolean started
    ) {
        var name = permission.displayName() != null ? permission.displayName() : permission.serviceName();
        var introduction = started
                ? "The measured power for \"%s\" exceeded the connection limits.".formatted(name)
                : "The measured power for \"%s\" is back within the connection limits.".formatted(name);

        return """
                %s

                Time: %s
                Measured power: %s kW
                Allowed limits: %s
                """.formatted(introduction,
                              timestamp,
                              format(netPower),
                              formatLimits(violation.minLimitKw(), violation.maxLimitKw()));
    }

    private String formatLimits(@Nullable BigDecimal min, @Nullable BigDecimal max) {
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
}
