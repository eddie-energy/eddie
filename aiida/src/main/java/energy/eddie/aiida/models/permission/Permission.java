// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.errors.permission.InvalidInboundPermissionException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Persistable;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "permission")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Permission implements Persistable<UUID> {
    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    @Schema(description = "Unique ID of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
    @JsonProperty
    private UUID permissionId;

    @Column(name = "eddie_id", updatable = false)
    @Schema(description = "Unique ID of the EDDIE application that created this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
    @JsonProperty
    private UUID eddieId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Status of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCEPTED")
    @JsonProperty
    private PermissionStatus status;

    @Column(name = "service_name")
    @Schema(description = "Name of the EP service that requested near real-time data.", example = "My Energy Visualization Service")
    @JsonProperty
    @Nullable
    private String serviceName;

    @Column(name = "display_name")
    @Schema(description = "User-chosen display name for this permission.", example = "My smart meter")
    @JsonProperty
    @Nullable
    private String displayName;

    @Column(name = "start_time")
    @Schema(description = "UTC timestamp when the data sharing should start.")
    @JsonProperty
    @Nullable
    private Instant startTime;

    @Nullable
    @Column(name = "expiration_time")
    @Schema(description = "UTC timestamp the data sharing should automatically stop. Must be after startTime.")
    @JsonProperty
    private Instant expirationTime;

    @Nullable
    @Column(name = "grant_time")
    @Schema(description = "UTC timestamp when the customer granted the sharing permission.")
    @JsonProperty
    private Instant grantTime;

    @Nullable
    @Column(name = "revoke_time")
    @Schema(description = "UTC timestamp when the permission was revoked or terminated. See status for the exact reason.")
    @JsonProperty
    private Instant revokeTime = null;

    @Nullable
    @Column(name = "connection_id")
    @Schema(description = "Connection ID as supplied by EDDIE/EP.", example = "SomeRandomString")
    @JsonIgnore
    private String connectionId;

    @Nullable
    @Column(name = "meter_id")
    @Schema(description = "ID of a physical smart meter asset used to map flexible connection agreements and control data source selection.", example = "003114735")
    @JsonProperty
    private String meterId;

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

    @Nullable
    @PrimaryKeyJoinColumn
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonProperty
    private MqttStreamingConfig mqttStreamingConfig;

    @Column(name = "handshake_url", nullable = false)
    @JsonIgnore
    private String handshakeUrl;

    @Column(name = "access_token", nullable = false)
    @JsonIgnore
    private String accessToken;

    @Nullable
    @JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id")
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JsonProperty
    private AiidaLocalDataNeed dataNeed;

    @Column(name = "user_id", nullable = false)
    @Schema(description = "UUID of the user that owns the permission.")
    @JsonProperty
    private UUID userId;

    @Nullable
    @JoinColumn(name = "data_source_id", referencedColumnName = "id")
    @ManyToOne(cascade = CascadeType.DETACH)
    @JsonProperty
    private DataSource dataSource;

    @Column(name = "inbound_message_format")
    @Enumerated(EnumType.STRING)
    @Schema(description = "Selected format for inbound permission messages.", example = "CIM_1_12")
    @JsonProperty
    @Nullable
    private InboundMessageFormat inboundMessageFormat;

    @Column(name = "transmission_enabled", nullable = false)
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
     * Not persisted, derived at runtime by {@code ConnectionStatusService}.
     */
    @Transient
    @Nullable
    private ConnectionStatus connectionStatus;

    @Transient
    @JsonIgnore
    private boolean isNew = true;

    /**
     * Create a new permission from the contents of the AIIDA code. Permissions are only persisted once the user accepted
     * them, therefore the status is set to {@link PermissionStatus#ACCEPTED}. The details of the permission have to be
     * set before it is persisted.
     */
    public Permission(UUID eddieId, UUID permissionId, String handshakeUrl, String accessToken, UUID userId) {
        this.eddieId = eddieId;
        this.permissionId = permissionId;
        this.handshakeUrl = handshakeUrl;
        this.accessToken = accessToken;
        this.status = PermissionStatus.ACCEPTED;
        this.userId = userId;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    @JsonCreator
    public Permission() {
    }

    @Override
    @JsonIgnore
    public UUID getId() {
        return permissionId;
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    /**
     * @return The UTC timestamp when this permission starts
     */
    public @Nullable Instant startTime() {
        return startTime;
    }

    /**
     * @return The UUID of the EDDIE application that created this permission.
     */
    public UUID eddieId() {
        return eddieId;
    }

    /**
     * @see #getId()
     * @return The UUID of this permission.
     */
    public UUID id() {
        return getId();
    }

    /**
     * @return The UTC timestamp when this permission expires and no more data should be shared.
     */
    public @Nullable Instant expirationTime() {
        return expirationTime;
    }

    /**
     * @return The UTC timestamp when the customer granted the permission. Will return null until the customer grants
     * the permission. Only available if status is {@link PermissionStatus#ACCEPTED} or a later status.
     */
    public @Nullable Instant grantTime() {
        return grantTime;
    }

    /**
     * @return The time at which either the EP terminated the permission or the customer revoked the permission. The
     * return value of {@link #status()} indicates which of the two cases occurred.
     */
    public @Nullable Instant revokeTime() {
        return revokeTime;
    }

    /**
     * @return The connectionId that was generated by the EDDIE framework and is sent along with each message to the EP.
     */
    public @Nullable String connectionId() {
        return connectionId;
    }

    /**
     * @return The optional physical meter ID provided by the eligible party.
     * Used to map flexible connection agreements and control data source selection in AIIDA.
     */
    public @Nullable String meterId() {
        return meterId;
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

    /**
     * @return The service name for which this permission is for.
     */
    public @Nullable String serviceName() {
        return serviceName;
    }

    /**
     * @return The user-chosen display name for this permission. Defaults to the data need's name.
     */
    public @Nullable String displayName() {
        return displayName;
    }

    public @Nullable InboundMessageFormat inboundMessageFormat() {
        return inboundMessageFormat;
    }

    /**
     * @return The current status of the permission.
     */
    public PermissionStatus status() {
        return status;
    }

    /**
     * @return The dynamic connection state, if it was derived for this instance, see
     * {@code ConnectionStatusService}. It is not persisted.
     */
    @JsonProperty(value = "connectionStatus", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Dynamic connection state, derived at runtime. Only present for accepted permissions.", example = "CONNECTED", accessMode = Schema.AccessMode.READ_ONLY)
    public @Nullable ConnectionStatus connectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(@Nullable ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    /**
     * @return The MQTT streaming configuration for this permission.
     */
    public @Nullable MqttStreamingConfig mqttStreamingConfig() {
        return mqttStreamingConfig;
    }

    /**
     * @return The URL for the handshake endpoint.
     */
    public String handshakeUrl() {
        return handshakeUrl;
    }

    /**
     * @return The access token for the permission.
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * @return The user ID associated with the permission.
     */
    public @Nullable UUID userId() {
        return userId;
    }

    /**
     * @return The data source for the permission.
     */
    public @Nullable DataSource dataSource() {
        return dataSource;
    }

    /**
     * @return The data need for the permission.
     */
    public @Nullable AiidaLocalDataNeed dataNeed() {
        return dataNeed;
    }

    /**
     * Defaults to {@code true} and is toggled by {@link PermissionCommand.SetTransmissionEnabled} commands from the EP.
     * @return Whether data transmission is currently enabled for this permission.
     */
    public boolean transmissionEnabled() {
        return transmissionEnabled;
    }

    public void setStatus(PermissionStatus newStatus) {
        this.status = requireNonNull(newStatus);
    }

    public void setStartTime(Instant startTime) {
        this.startTime = requireNonNull(startTime);
    }

    public void setExpirationTime(Instant expirationTime) {
        this.expirationTime = requireNonNull(expirationTime);
    }

    public void setGrantTime(Instant grantTime) {
        this.grantTime = requireNonNull(grantTime);
    }

    /**
     * Set the UTC timestamp when either the EP terminated the permission or the customer revoked the permission. Use
     * {@link #setStatus(PermissionStatus)} to indicate which of the two cases occurred.
     *
     * @param revokeTime The revocation or termination timestamp.
     */
    public void setRevokeTime(Instant revokeTime) {
        requireNonNull(revokeTime);

        if (grantTime != null && revokeTime.isBefore(grantTime))
            throw new IllegalArgumentException("revokeTime must not be before grantTime.");

        this.revokeTime = revokeTime;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = requireNonNull(connectionId);
    }

    public void setMeterId(@Nullable String meterId) {
        this.meterId = meterId;
    }

    public void setMinLimitKw(@Nullable BigDecimal minLimitKw) {
        this.minLimitKw = minLimitKw;
    }

    public void setMaxLimitKw(@Nullable BigDecimal maxLimitKw) {
        this.maxLimitKw = maxLimitKw;
    }

    public void setDisplayName(String displayName) {
        this.displayName = requireNonNull(displayName);
    }

    public void setMqttStreamingConfig(MqttStreamingConfig mqttStreamingConfig) {
        this.mqttStreamingConfig = requireNonNull(mqttStreamingConfig);
    }

    /**
     * Initializes the permission from the data need by deriving {@link #serviceName}, {@link #displayName}
     * and {@link #inboundMessageFormat} from it.
     */
    public void initializeFromDataNeed(AiidaLocalDataNeed dataNeed) {
        this.dataNeed = requireNonNull(dataNeed);
        deriveServiceName(dataNeed);
        deriveDisplayName(dataNeed);
        deriveInboundMessageFormat(dataNeed);
    }

    public void setDataSource(@Nullable DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void updateInboundMessageFormat(@NonNull InboundMessageFormat inboundMessageFormat)
            throws InvalidInboundPermissionException {
        if (!(dataNeed instanceof InboundAiidaLocalDataNeed)) {
            throw new InvalidInboundPermissionException(permissionId);
        }

        this.inboundMessageFormat = inboundMessageFormat;
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
    @Nullable
    @JsonProperty
    @JsonSerialize(using = CronExpressionSerializer.class)
    public CronExpression effectiveTransmissionSchedule() {
        if (transmissionSchedule != null) {
            return transmissionSchedule;
        }
        return dataNeed != null ? dataNeed.transmissionSchedule() : null;
    }

    /**
     * {@code true} if the permission allows the eligible party to send connection limits.
     * @return Whether the permission supports connection limit features such as monitoring.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Whether the permission supports connection limit features such as monitoring.", example = "true")
    public boolean supportsConnectionLimits() {
        return dataNeed instanceof InboundAiidaLocalDataNeed && dataNeed.supportsConnectionLimits();
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    private void deriveServiceName(AiidaLocalDataNeed dataNeed) {
        this.serviceName = dataNeed.name();
    }

    private void deriveDisplayName(AiidaLocalDataNeed dataNeed) {
        this.displayName = dataNeed.name();
    }

    private void deriveInboundMessageFormat(AiidaLocalDataNeed dataNeed) {
        if (dataNeed instanceof InboundAiidaLocalDataNeed && this.inboundMessageFormat == null) {
            this.inboundMessageFormat = InboundMessageFormat.CIM_1_12;
        } else {
            this.inboundMessageFormat = null;
        }
    }
}
