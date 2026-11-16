// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Permission that allows AIIDA to stream data of a data source selected by the user to the eligible party.
 */
@Entity
@DiscriminatorValue(OutboundPermission.DISCRIMINATOR_VALUE)
public class OutboundPermission extends Permission {
    public static final String DISCRIMINATOR_VALUE = "outbound";

    @Column(name = "transmission_enabled")
    @JsonProperty
    private boolean transmissionEnabled = true;

    @Nullable
    @Column(name = "transmission_schedule")
    @Convert(converter = CronExpressionConverter.class)
    @JsonProperty
    @JsonSerialize(using = CronExpressionSerializer.class)
    @JsonDeserialize(using = CronExpressionDeserializer.class)
    private CronExpression transmissionSchedule;

    /**
     * Create a new outbound permission, see {@link Permission}.
     */
    @SuppressWarnings("java:S107") // see Permission
    public OutboundPermission(
            UUID eddieId,
            UUID permissionId,
            String handshakeUrl,
            String accessToken,
            UUID userId,
            OutboundAiidaLocalDataNeed dataNeed,
            String connectionId,
            Instant startTime,
            Instant expirationTime,
            Instant grantTime
    ) {
        super(eddieId,
              permissionId,
              handshakeUrl,
              accessToken,
              userId,
              dataNeed,
              connectionId,
              startTime,
              expirationTime,
              grantTime);
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected OutboundPermission() {
        super();
    }

    @Override
    public OutboundAiidaLocalDataNeed dataNeed() {
        return (OutboundAiidaLocalDataNeed) super.dataNeed();
    }

    /**
     * Defaults to {@code true} and is toggled by {@link PermissionCommand.SetTransmissionEnabled} commands from the EP.
     * @return Whether data transmission is currently enabled for this permission.
     */
    public boolean transmissionEnabled() {
        return transmissionEnabled;
    }

    public void setTransmissionEnabled(boolean transmissionEnabled) {
        this.transmissionEnabled = transmissionEnabled;
    }

    public void setTransmissionSchedule(CronExpression transmissionSchedule) {
        this.transmissionSchedule = requireNonNull(transmissionSchedule);
    }

    /**
     * @return The schedule that should be used to stream data for this permission: the permission's own
     * transmission schedule if set, otherwise the data need's schedule as a fallback
     */
    @JsonProperty
    @JsonSerialize(using = CronExpressionSerializer.class)
    public CronExpression effectiveTransmissionSchedule() {
        if (transmissionSchedule != null) {
            return transmissionSchedule;
        }
        return dataNeed().transmissionSchedule();
    }
}
