// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public class EnerginetPermissionRequestBuilder {
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private String meteringPoint;
    private String refreshToken;
    private LocalDate start;
    private LocalDate end;
    private Granularity granularity;
    private String accessToken;
    private PermissionProcessStatus status;
    private ZonedDateTime created;
    private LocalDate latestMeterReadingEndDate;

    public EnerginetPermissionRequestBuilder setPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public EnerginetPermissionRequestBuilder setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public EnerginetPermissionRequestBuilder setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public EnerginetPermissionRequestBuilder setMeteringPoint(String meteringPoint) {
        this.meteringPoint = meteringPoint;
        return this;
    }

    public EnerginetPermissionRequestBuilder setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public EnerginetPermissionRequestBuilder setStart(LocalDate start) {
        this.start = start;
        return this;
    }

    public EnerginetPermissionRequestBuilder setEnd(LocalDate end) {
        this.end = end;
        return this;
    }

    public EnerginetPermissionRequestBuilder setGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public EnerginetPermissionRequestBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public EnerginetPermissionRequestBuilder setStatus(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public EnerginetPermissionRequestBuilder setCreated(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public EnerginetPermissionRequestBuilder setLatestMeterReadingEndDate(LocalDate latestMeterReadingEndDate) {
        this.latestMeterReadingEndDate = latestMeterReadingEndDate;
        return this;
    }

    public EnerginetPermissionRequest build() {
        return new EnerginetPermissionRequest(permissionId,
                                              connectionId,
                                              dataNeedId,
                                              meteringPoint,
                                              refreshToken,
                                              start,
                                              end,
                                              granularity,
                                              accessToken,
                                              status,
                                              created,
                                              latestMeterReadingEndDate);
    }
}