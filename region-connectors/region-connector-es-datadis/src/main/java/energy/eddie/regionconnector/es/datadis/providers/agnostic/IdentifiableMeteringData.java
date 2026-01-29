// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;

public record IdentifiableMeteringData(
        EsPermissionRequest permissionRequest,
        IntermediateMeteringData intermediateMeteringData
) implements MeterReadingEndDate, IdentifiablePayload<EsPermissionRequest, IntermediateMeteringData> {
    @Override
    public LocalDate meterReadingEndDate() {
        return intermediateMeteringData().end();
    }

    @Override
    public IntermediateMeteringData payload() {
        return intermediateMeteringData;
    }
}
