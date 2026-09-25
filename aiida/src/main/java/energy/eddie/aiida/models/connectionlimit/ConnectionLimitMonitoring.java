// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Maps a data source to monitor for limit violation to a permission receiving connection limits.
 */
@Entity
@Table(name = "connection_limit_monitoring")
public class ConnectionLimitMonitoring {

    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    @JsonProperty
    @Schema(description = "Permission ID the monitoring assignment belongs to.", example = "9921f327-f341-4bea-bf08-3cf2acc65bf3")
    private UUID permissionId;

    @Column(name = "data_source_id", nullable = false)
    @JsonProperty
    @Schema(description = "ID of the data source monitored for connection limit violations.", example = "51d0a13e-688a-454d-acab-7a6b2951cde2")
    private UUID dataSourceId;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected ConnectionLimitMonitoring() {
    }

    public ConnectionLimitMonitoring(UUID permissionId, UUID dataSourceId) {
        this.permissionId = permissionId;
        this.dataSourceId = dataSourceId;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public UUID dataSourceId() {
        return dataSourceId;
    }
}
