// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.data;

import jakarta.persistence.*;

@Entity
@Table(name = "status_messages", schema = "admin_console")
@SuppressWarnings("NullAway") // Needed for JPA
public class StatusMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Column(name = "permission_id", nullable = false)
    private final String permissionId;

    @Column(name = "region_connector_id")
    private final String regionConnectorId;

    @Column(name = "data_need_id")
    private final String dataNeedId;

    @Column(name = "country")
    private final String country;

    @Column(name = "dso")
    private final String dso;

    @Column(name = "creation_date")
    private final String creationDate;

    @Column(name = "start_date")
    private final String startDate;

    @Column(name = "end_date")
    private final String endDate;

    @Column(name = "status")
    private final String status;

    @Column(name = "description")
    private final String description;

    @Column(name = "reason")
    private final String reason;

    // Constructors
    public StatusMessage(
            String permissionId,
            String regionConnectorId,
            String dataNeedId,
            String country,
            String dso,
            String creationDate,
            String startDate,
            String endDate,
            String status,
            String description,
            String reason
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.regionConnectorId = regionConnectorId;
        this.dataNeedId = dataNeedId;
        this.country = country;
        this.dso = dso;
        this.creationDate = creationDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.description = description;
        this.reason = reason;
    }

    protected StatusMessage() {
        this.id = null;
        this.permissionId = null;
        this.regionConnectorId = null;
        this.dataNeedId = null;
        this.country = null;
        this.dso = null;
        this.creationDate = null;
        this.startDate = null;
        this.endDate = null;
        this.status = null;
        this.description = null;
        this.reason = null;
    }

    public Long getId() {
        return id;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getRegionConnectorId() {
        return regionConnectorId;
    }

    public String getDataNeedId() {
        return dataNeedId;
    }

    public String getCountry() {
        return country;
    }

    public String getDso() {
        return dso;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getReason() {
        return reason;
    }
}