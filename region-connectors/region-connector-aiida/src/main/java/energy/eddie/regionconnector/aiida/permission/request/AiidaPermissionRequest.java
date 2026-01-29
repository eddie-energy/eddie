// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.permission.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;


@Entity
@Table(schema = AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID, name = "aiida_permission_request_view")
public class AiidaPermissionRequest implements AiidaPermissionRequestInterface {
    private static final AiidaDataSourceInformation DATA_SOURCE_INFORMATION = new AiidaDataSourceInformation();
    @Id
    @Column(name = "permission_id")
    @JsonProperty(value = "permission_id")
    private String permissionId;
    @Column(name = "connection_id")
    @JsonProperty(value = "connection_id")
    private String connectionId;
    @Column(name = "data_need_id")
    @JsonProperty(value = "data_need_id")
    private String dataNeedId;
    @Column(name = "permission_start")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate start;
    @Column(name = "permission_end")
    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PermissionProcessStatus status;
    @Column(name = "termination_topic")
    @Nullable
    private String terminationTopic;
    @Column(name = "message")
    @Nullable
    private String message;
    @Column(name = "created")
    private Instant created;
    @Column(name = "aiida_id")
    @Nullable
    private UUID aiidaId;

    @SuppressWarnings("NullAway") // Needed for JPA
    protected AiidaPermissionRequest() {
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
        return DATA_SOURCE_INFORMATION;
    }

    @Override
    @SuppressWarnings({"NullAway", "DataFlowIssue"})
    public ZonedDateTime created() {
        return created.atZone(ZoneOffset.UTC);
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
    @Nullable
    public String terminationTopic() {
        return terminationTopic;
    }

    @Override
    @Nullable
    public String message() {
        return message;
    }

    @Override
    @Nullable
    public UUID aiidaId() {
        return aiidaId;
    }
}
