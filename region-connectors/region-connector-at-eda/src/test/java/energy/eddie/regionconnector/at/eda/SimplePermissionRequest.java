// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public record SimplePermissionRequest(String permissionId,
                                      String connectionId,
                                      String dataNeedId,
                                      String cmRequestId,
                                      String conversationId,
                                      String dsoId,
                                      Optional<String> meteringPointId,
                                      LocalDate start,
                                      LocalDate end,
                                      PermissionProcessStatus status,
                                      Optional<String> consentId,
                                      @Nullable AllowedGranularity granularity
) implements AtPermissionRequest {

    public SimplePermissionRequest(String permissionId, String connectionId, String dataNeedId) {
        this(permissionId,
             connectionId,
             dataNeedId,
             null,
             null,
             null,
             Optional.empty(),
             null,
             null,
             null,
             Optional.empty());
    }

    public SimplePermissionRequest(
            String permissionId, String connectionId, String dataNeedId, String cmRequestId,
            String conversationId, PermissionProcessStatus status
    ) {
        this(permissionId, connectionId, dataNeedId, cmRequestId, conversationId, null, Optional.empty(), null, null,
             status, Optional.empty());
    }

    public SimplePermissionRequest(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String cmRequestId,
            String conversationId,
            String dsoId,
            Optional<String> meteringPointId,
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus status,
            Optional<String> consentId
    ) {
        this(permissionId, connectionId, dataNeedId, cmRequestId, conversationId, dsoId, meteringPointId, start, end,
             status, consentId, AllowedGranularity.PT15M);
    }

    @Override
    public Optional<String> consentId() {
        return consentId;
    }

    @Override
    public String message() {
        return null;
    }

    @Override
    public AllowedGranularity granularity() {
        return granularity;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new EdaDataSourceInformation(dsoId);
    }

    @Override
    public ZonedDateTime created() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtPermissionRequest that)) return false;
        return permissionId.equals(that.permissionId()) && connectionId.equals(
                that.connectionId()) && cmRequestId.equals(that.cmRequestId()) && conversationId.equals(
                that.conversationId()) && status == that.status();
    }
}
