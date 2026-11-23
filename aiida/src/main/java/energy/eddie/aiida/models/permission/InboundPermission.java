// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Permission that allows the eligible party to send data to AIIDA. Accepting it creates a dedicated inbound data
 * source.
 */
@Entity
@DiscriminatorValue(InboundPermission.DISCRIMINATOR_VALUE)
public class InboundPermission extends Permission {
    public static final String DISCRIMINATOR_VALUE = "inbound";

    @Nullable
    @Formula("(SELECT d.min_limit_kw FROM connection_limit_default d WHERE d.permission_id = permission_id AND d.replaced_at IS NULL ORDER BY d.start DESC, d.id DESC LIMIT 1)")
    @JsonProperty
    @Schema(description = "Optional current default minimum energy consumption or production limit in kilowatts for controllable units.", example = "1.0")
    private BigDecimal minLimitKw;

    @Nullable
    @Formula("(SELECT d.max_limit_kw FROM connection_limit_default d WHERE d.permission_id = permission_id AND d.replaced_at IS NULL ORDER BY d.start DESC, d.id DESC LIMIT 1)")
    @Schema(description = "Optional current default maximum energy consumption or production limit in kilowatts for controllable units.", example = "10.0")
    @JsonProperty
    private BigDecimal maxLimitKw;

    @Nullable
    @Column(name = "monitoring_data_source_id")
    private UUID monitoringDataSourceId;

    @Column(name = "inbound_message_format")
    @Enumerated(EnumType.STRING)
    @Schema(description = "Selected format for inbound permission messages.", example = "CIM_1_12")
    @JsonProperty
    private InboundMessageFormat inboundMessageFormat = InboundMessageFormat.CIM_1_12;

    /**
     * Create a new inbound permission, see {@link Permission}. The inbound message format defaults to
     * {@link InboundMessageFormat#CIM_1_12}.
     */
    @SuppressWarnings("java:S107") // see Permission
    public InboundPermission(
            UUID eddieId,
            UUID permissionId,
            String handshakeUrl,
            String accessToken,
            UUID userId,
            InboundAiidaLocalDataNeed dataNeed,
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
    protected InboundPermission() {
        super();
    }

    @Override
    public InboundAiidaLocalDataNeed dataNeed() {
        return (InboundAiidaLocalDataNeed) super.dataNeed();
    }

    @Override
    public @Nullable InboundDataSource dataSource() {
        return (InboundDataSource) super.dataSource();
    }

    /**
     * Resolved from the current connection limit default with the newest start time in the database.
     * @return Optional current default minimum energy consumption or production limit in kilowatts for controllable units.
     */
    public @Nullable BigDecimal minLimitKw() {
        return minLimitKw;
    }

    /**
     * Resolved from the current connection limit default with the newest start time in the database.
     * @return Optional current default maximum energy consumption or production limit in kilowatts for controllable units.
     */
    public @Nullable BigDecimal maxLimitKw() {
        return maxLimitKw;
    }

    public void setMinLimitKw(@Nullable BigDecimal minLimitKw) {
        this.minLimitKw = minLimitKw;
    }

    public void setMaxLimitKw(@Nullable BigDecimal maxLimitKw) {
        this.maxLimitKw = maxLimitKw;
    }

    /**
     * @return The data source monitored for connection limit violations, or null if none is assigned.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public @Nullable UUID monitoringDataSourceId() {
        return monitoringDataSourceId;
    }

    /**
     * Assigns the data source to monitor for connection limit violations, or clears the assignment if null.
     */
    public void updateMonitoringDataSource(@Nullable UUID dataSourceId) {
        monitoringDataSourceId = dataSourceId;
    }

    public InboundMessageFormat inboundMessageFormat() {
        return inboundMessageFormat;
    }

    public void updateInboundMessageFormat(InboundMessageFormat inboundMessageFormat) {
        this.inboundMessageFormat = inboundMessageFormat;
    }

    /**
     * {@code true} if the permission allows the eligible party to send connection limits.
     * @return Whether the permission supports connection limit features such as monitoring.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Whether the permission supports connection limit features such as monitoring.", example = "true")
    public boolean supportsConnectionLimits() {
        return dataNeed().supportsConnectionLimits();
    }
}
