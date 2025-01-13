package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "permission")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Permission {
    @Schema(description = "Unique ID of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
    @Id
    @Column(nullable = false, updatable = false, name = "permission_id")
    @JsonProperty
    private UUID permissionId;
    @Schema(description = "Unique ID of the EDDIE application that created this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "a4dc1bad-b9fe-47ae-9336-690cfb4aada9")
    @Column(updatable = false, name = "eddie_id")
    @JsonProperty
    private UUID eddieId;
    @Schema(description = "Status of this permission.", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACCEPTED")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty
    private PermissionStatus status;
    @Schema(description = "Name of the EP service that requested near real-time data.", example = "My Energy Visualization Service")
    @Column(nullable = false)
    @JsonProperty
    private String serviceName;
    @Schema(description = "UTC timestamp when the data sharing should start.")
    @Column
    @JsonProperty
    @Nullable
    private Instant startTime;
    @Schema(description = "UTC timestamp the data sharing should automatically stop. Must be after startTime.")
    @Column
    @JsonProperty
    @Nullable
    private Instant expirationTime;
    @Schema(description = "UTC timestamp when the customer granted the sharing permission.")
    @Column
    @JsonProperty
    @Nullable
    private Instant grantTime;
    @Schema(description = "UTC timestamp when the permission was revoked or terminated. See status for the exact reason.")
    @JsonProperty
    @Nullable
    private Instant revokeTime = null;
    @Schema(description = "Connection ID as supplied by EDDIE/EP.", example = "SomeRandomString")
    @Column
    @JsonIgnore
    @Nullable
    private String connectionId;
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    @JsonIgnore
    @Nullable
    private MqttStreamingConfig mqttStreamingConfig;
    @Column(nullable = false)
    @JsonIgnore
    private String handshakeUrl;
    @Column(nullable = false)
    @JsonIgnore
    private String accessToken;
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id", insertable = false)
    @JsonProperty
    @Nullable
    private AiidaLocalDataNeed dataNeed;
    @Schema(description = "UUID of the user that owns the permission.")
    @Column
    @JsonProperty
    @Nullable
    private UUID userId;

    /**
     * Create a new permission from the contents of the QR code. The status will be set to
     * {@link PermissionStatus#CREATED}.
     */
    public Permission(QrCodeDto qrCodeDto, UUID userId) {
        this.eddieId = qrCodeDto.eddieId();
        this.permissionId = qrCodeDto.permissionId();
        this.serviceName = qrCodeDto.serviceName();
        this.handshakeUrl = qrCodeDto.handshakeUrl();
        this.accessToken = qrCodeDto.accessToken();
        this.status = PermissionStatus.CREATED;
        this.userId = userId;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    public Permission() {
    }

    /**
     * Returns the UUID of the EDDIE application that created this permission.
     */
    public UUID eddieId() {
        return eddieId;
    }

    /**
     * Returns the UUID of this permission.
     */
    public UUID permissionId() {
        return permissionId;
    }

    /**
     * Returns the UTC start timestamp for sharing data of this permission. Only available if status is
     * {@link PermissionStatus#FETCHED_DETAILS} or a later status.
     */
    public @Nullable Instant startTime() {
        return startTime;
    }

    /**
     * Returns the UTC timestamp when this permission expires and no more data should be shared. Only available if
     * status is {@link PermissionStatus#FETCHED_DETAILS} or a later status.
     */
    public @Nullable Instant expirationTime() {
        return expirationTime;
    }

    /**
     * Returns the UTC timestamp when the customer granted the permission. Will return null until the customer grants
     * the permission. Only available if status is {@link PermissionStatus#ACCEPTED} or a later status.
     */
    public @Nullable Instant grantTime() {
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
     * Only available if status is {@link PermissionStatus#FETCHED_DETAILS} or a later status.
     */
    public @Nullable String connectionId() {
        return connectionId;
    }

    /**
     * Returns the service name for which this permission is for.
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * Returns the current status of the permission.
     */
    public PermissionStatus status() {
        return status;
    }

    /**
     * Only available if status is {@link PermissionStatus#FETCHED_MQTT_CREDENTIALS} or a later status.
     */
    public @Nullable MqttStreamingConfig mqttStreamingConfig() {
        return mqttStreamingConfig;
    }

    public String handshakeUrl() {
        return handshakeUrl;
    }

    public String accessToken() {
        return accessToken;
    }

    public @Nullable UUID userId() {
        return userId;
    }

    /**
     * Only available if status is {@link PermissionStatus#FETCHED_DETAILS} or a later status.
     */
    public @Nullable AiidaLocalDataNeed dataNeed() {
        return dataNeed;
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

    public void setMqttStreamingConfig(MqttStreamingConfig mqttStreamingConfig) {
        this.mqttStreamingConfig = requireNonNull(mqttStreamingConfig);
    }

    public void setDataNeed(AiidaLocalDataNeed dataNeed) {
        this.dataNeed = requireNonNull(dataNeed);
    }
}
