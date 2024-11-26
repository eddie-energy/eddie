package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@SuppressWarnings("NullAway") // Needed for JPA
@Entity
@Table(schema = "at_eda", name = "eda_permission_request")
public class EdaPermissionRequest implements AtPermissionRequest {
    @Id
    private final String permissionId;
    private final String connectionId;
    private final String cmRequestId;
    private final String conversationId;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Nullable
    @Column(name = "permission_end")
    private final LocalDate end;
    private final String dataNeedId;
    @Embedded
    private final EdaDataSourceInformation dataSourceInformation;
    @Nullable
    private final String meteringPointId;
    private final String message;
    @Nullable
    @Column(name = "cm_consent_id")
    private final String consentId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final AllowedGranularity granularity;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;
    private final ZonedDateTime created;

    public EdaPermissionRequest(
            String connectionId, String dataNeedId, CCMORequest ccmoRequest,
            AllowedGranularity granularity, PermissionProcessStatus status, String message,
            String consentId
    ) {
        this(connectionId, UUID.randomUUID().toString(), dataNeedId, ccmoRequest, granularity, status, message,
             consentId);
    }

    @SuppressWarnings("java:S107")
    public EdaPermissionRequest(
            String connectionId, String permissionId, String dataNeedId, CCMORequest ccmoRequest,
            AllowedGranularity granularity, PermissionProcessStatus status, String message,
            @Nullable String consentId
    ) {
        this(connectionId, permissionId, dataNeedId, ccmoRequest.cmRequestId(), ccmoRequest.messageId(),
             ccmoRequest.meteringPointId()
                        .orElse(null), ccmoRequest.dsoId(), ccmoRequest.start(), ccmoRequest.end().orElse(null),
             granularity, status, message, consentId, ZonedDateTime.now(AT_ZONE_ID));
    }

    @SuppressWarnings("java:S107")
    public EdaPermissionRequest(
            String connectionId, String permissionId, String dataNeedId, String cmRequestId,
            String conversationId, @Nullable String meteringPointId, String dsoId,
            LocalDate start, @Nullable LocalDate end, AllowedGranularity granularity,
            PermissionProcessStatus status, String message, @Nullable String consentId,
            ZonedDateTime created
    ) {
        this.connectionId = connectionId;
        this.permissionId = permissionId;
        this.dataNeedId = dataNeedId;
        this.cmRequestId = cmRequestId;
        this.conversationId = conversationId;
        this.meteringPointId = meteringPointId;
        this.dataSourceInformation = new EdaDataSourceInformation(dsoId);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.status = status;
        this.message = message;
        this.consentId = consentId;
        this.created = created;
    }

    // protected no-args ctor needed for JPA and reflections
    protected EdaPermissionRequest() {
        this(null, null, null, null, null, null,
             null, null, null, null, null, null, null, null);
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return dataSourceInformation;
    }

    @Override
    public ZonedDateTime created() {
        return created;
    }

    @Override
    public LocalDate start() {
        return start;
    }

    @Override
    public LocalDate end() {
        return end;
    }

    @Override
    public String cmRequestId() {
        return cmRequestId;
    }

    @Override
    public String conversationId() {
        return conversationId;
    }

    @Override
    public Optional<String> meteringPointId() {
        return Optional.ofNullable(meteringPointId);
    }

    @Override
    public Optional<String> consentId() {
        return Optional.ofNullable(consentId);
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public AllowedGranularity granularity() {
        return granularity;
    }
}
