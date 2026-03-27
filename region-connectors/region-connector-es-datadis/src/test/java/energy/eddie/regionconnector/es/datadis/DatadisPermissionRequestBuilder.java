// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class DatadisPermissionRequestBuilder {
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private LocalDate start;
    private LocalDate end;
    private Granularity granularity;
    private PermissionProcessStatus status;
    private ZonedDateTime created;
    private String nif;
    private String meteringPointId;
    private String errorMessage;
    private DistributorCode distributorCode;
    private PointType pointType;
    private LocalDate latestMeterReadingEndDate;
    private boolean productionSupport;
    private AllowedGranularity allowedGranularity;
    private UUID bundleId;
    private String firstname;
    private String surname;

    public DatadisPermissionRequestBuilder setPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public DatadisPermissionRequestBuilder setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public DatadisPermissionRequestBuilder setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public DatadisPermissionRequestBuilder setStart(LocalDate start) {
        this.start = start;
        return this;
    }

    public DatadisPermissionRequestBuilder setEnd(LocalDate end) {
        this.end = end;
        return this;
    }

    public DatadisPermissionRequestBuilder setGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public DatadisPermissionRequestBuilder setStatus(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public DatadisPermissionRequestBuilder setCreated(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public DatadisPermissionRequestBuilder setNif(String nif) {
        this.nif = nif;
        return this;
    }

    public DatadisPermissionRequestBuilder setMeteringPointId(String meteringPointId) {
        this.meteringPointId = meteringPointId;
        return this;
    }

    public DatadisPermissionRequestBuilder setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }


    public DatadisPermissionRequestBuilder setDistributorCode(DistributorCode distributorCode) {
        this.distributorCode = distributorCode;
        return this;
    }

    public DatadisPermissionRequestBuilder setPointType(PointType pointType) {
        this.pointType = pointType;
        return this;
    }

    public DatadisPermissionRequestBuilder setLatestMeterReadingEndDate(LocalDate latestMeterReadingEndDate) {
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        return this;
    }

    public DatadisPermissionRequestBuilder setProductionSupport(boolean productionSupport) {
        this.productionSupport = productionSupport;
        return this;
    }

    public DatadisPermissionRequestBuilder setAllowedGranularity(AllowedGranularity allowedGranularity) {
        this.allowedGranularity = allowedGranularity;
        return this;
    }

    public DatadisPermissionRequestBuilder setBundleId(UUID bundleId) {
        this.bundleId = bundleId;
        return this;
    }

    public DatadisPermissionRequestBuilder setFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    public DatadisPermissionRequestBuilder setSurname(String surname) {
        this.surname = surname;
        return this;
    }

    public DatadisPermissionRequest build() {
        return new DatadisPermissionRequest(
                permissionId,
                connectionId,
                dataNeedId,
                granularity,
                nif,
                meteringPointId,
                start,
                end,
                distributorCode,
                Optional.ofNullable(pointType).map(PointType::value).orElse(null),
                latestMeterReadingEndDate,
                status,
                errorMessage,
                productionSupport,
                created,
                allowedGranularity,
                bundleId,
                firstname,
                surname
        );
    }
}
