// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "permission")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Permission implements Persistable<UUID> {
    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    @Schema(description = "Unique ID of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
    @JsonProperty
    private UUID permissionId;

    @Column(name = "eddie_id", nullable = false, updatable = false)
    @Schema(description = "Unique ID of the EDDIE application that created this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
    @JsonProperty
    private UUID eddieId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "Status of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCEPTED")
    @JsonProperty
    private PermissionStatus status;

    @Column(name = "service_name", nullable = false)
    @Schema(description = "Name of the EP service that requested near real-time data.", example = "My Energy Visualization Service")
    @JsonProperty
    private String serviceName;

    @Column(name = "display_name", nullable = false)
    @Schema(description = "User-chosen display name for this permission.", example = "My smart meter")
    @JsonProperty
    private String displayName;

    @Column(name = "start_time", nullable = false, updatable = false)
    @Schema(description = "UTC timestamp when the data sharing should start.")
    @JsonProperty
    private Instant startTime;

    @Column(name = "expiration_time", nullable = false, updatable = false)
    @Schema(description = "UTC timestamp the data sharing should automatically stop. Must be after startTime.")
    @JsonProperty
    private Instant expirationTime;

    @Column(name = "grant_time", nullable = false, updatable = false)
    @Schema(description = "UTC timestamp when the customer granted the sharing permission.")
    @JsonProperty
    private Instant grantTime;

    @Nullable
    @Column(name = "revoke_time")
    @Schema(description = "UTC timestamp when the permission was revoked or terminated. See status for the exact reason.")
    @JsonProperty
    private Instant revokeTime = null;

    @Column(name = "connection_id", nullable = false, updatable = false)
    @Schema(description = "Connection ID as supplied by EDDIE/EP.", example = "SomeRandomString")
    @JsonIgnore
    private String connectionId;

    @Nullable
    @Column(name = "meter_id")
    @Schema(description = "ID of a physical smart meter asset used to map flexible connection agreements and control data source selection.", example = "003114735")
    @JsonProperty
    private String meterId;

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

    @JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id", nullable = false, updatable = false)
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

    @Transient
    @JsonIgnore
    private boolean isNew = true;

    /**
     * Create a new permission from the contents of the AIIDA code and the details fetched from the EDDIE framework.
     * Permissions are only persisted once the user accepted them, therefore the status is set to
     * {@link PermissionStatus#ACCEPTED}. The service name and display name are derived from the data need.
     */
    @SuppressWarnings("java:S107") // Permissions are created from the AIIDA code and the fetched details at once
    protected Permission(
            UUID eddieId,
            UUID permissionId,
            String handshakeUrl,
            String accessToken,
            UUID userId,
            AiidaLocalDataNeed dataNeed,
            String connectionId,
            Instant startTime,
            Instant expirationTime,
            Instant grantTime
    ) {
        this.eddieId = requireNonNull(eddieId);
        this.permissionId = requireNonNull(permissionId);
        this.handshakeUrl = requireNonNull(handshakeUrl);
        this.accessToken = requireNonNull(accessToken);
        this.status = PermissionStatus.ACCEPTED;
        this.userId = requireNonNull(userId);
        this.dataNeed = requireNonNull(dataNeed);
        this.connectionId = requireNonNull(connectionId);
        this.startTime = requireNonNull(startTime);
        this.expirationTime = requireNonNull(expirationTime);
        this.grantTime = requireNonNull(grantTime);
        this.serviceName = dataNeed.name();
        this.displayName = dataNeed.name();
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected Permission() {
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
     * Returns the UTC timestamp when this permission starts.
     */
    public Instant startTime() {
        return startTime;
    }

    /**
     * Returns the UUID of the EDDIE application that created this permission.
     */
    public UUID eddieId() {
        return eddieId;
    }

    /**
     * Returns the UUID of this permission.
     *
     * @see #getId()
     */
    public UUID id() {
        return getId();
    }

    /**
     * Returns the UTC timestamp when this permission expires and no more data should be shared.
     */
    public Instant expirationTime() {
        return expirationTime;
    }

    /**
     * Returns the UTC timestamp when the customer granted the permission, i.e. when the permission was built to be
     * accepted.
     */
    public Instant grantTime() {
        return grantTime;
    }

    /**
     * Returns the time at which either the EP terminated the permission or the customer revoked the permission. The
     * return value of {@link #status()} indicates which of the two cases occurred.
     */
    public @Nullable Instant revokeTime() {
        return revokeTime;
    }

    /**
     * Returns the connectionId that was generated by the EDDIE framework and is sent along with each message to the EP.
     */
    public String connectionId() {
        return connectionId;
    }

    /**
     * Returns the optional physical meter ID provided by the eligible party.
     * Used to map flexible connection agreements and control data source selection in AIIDA.
     */
    public @Nullable String meterId() {
        return meterId;
    }

    /**
     * Returns the service name for which this permission is for.
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * Returns the user-chosen display name for this permission. Defaults to the data need's name.
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the current status of the permission.
     */
    public PermissionStatus status() {
        return status;
    }

    /**
     * Returns the MQTT streaming configuration for this permission.
     */
    public @Nullable MqttStreamingConfig mqttStreamingConfig() {
        return mqttStreamingConfig;
    }

    /**
     * Returns the URL for the handshake endpoint.
     */
    public String handshakeUrl() {
        return handshakeUrl;
    }

    /**
     * Returns the access token for the permission.
     */
    public String accessToken() {
        return accessToken;
    }

    /**
     * Returns the user ID associated with the permission.
     */
    public UUID userId() {
        return userId;
    }

    /**
     * Returns the data source for the permission.
     */
    public @Nullable DataSource dataSource() {
        return dataSource;
    }

    /**
     * Returns the data need for the permission.
     */
    public AiidaLocalDataNeed dataNeed() {
        return dataNeed;
    }

    public void setStatus(PermissionStatus newStatus) {
        this.status = requireNonNull(newStatus);
    }

    /**
     * Set the UTC timestamp when either the EP terminated the permission or the customer revoked the permission. Use
     * {@link #setStatus(PermissionStatus)} to indicate which of the two cases occurred.
     *
     * @param revokeTime The revocation or termination timestamp.
     */
    public void setRevokeTime(Instant revokeTime) {
        requireNonNull(revokeTime);

        if (revokeTime.isBefore(grantTime))
            throw new IllegalArgumentException("revokeTime must not be before grantTime.");

        this.revokeTime = revokeTime;
    }

    public void setMeterId(@Nullable String meterId) {
        this.meterId = meterId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = requireNonNull(displayName);
    }

    public void setMqttStreamingConfig(MqttStreamingConfig mqttStreamingConfig) {
        this.mqttStreamingConfig = requireNonNull(mqttStreamingConfig);
    }

    public void setDataSource(@Nullable DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
