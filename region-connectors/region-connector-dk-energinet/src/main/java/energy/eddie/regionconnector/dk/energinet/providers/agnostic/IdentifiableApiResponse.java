// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

public record IdentifiableApiResponse(
        DkEnerginetPermissionRequest permissionRequest,
        MyEnergyDataMarketDocumentResponse apiResponse
) implements MeterReadingEndDate, IdentifiablePayload<DkEnerginetPermissionRequest, MyEnergyDataMarketDocumentResponse> {
    @Override
    public LocalDate meterReadingEndDate() {
        @SuppressWarnings("DataFlowIssue") // IdentifiableApiResponseFilter ensures that none of these getters return null
        var timeInterval = apiResponse.getMyEnergyDataMarketDocument().getPeriodTimeInterval().getEnd();
        return parseZonedDateTime(timeInterval);
    }

    private static LocalDate parseZonedDateTime(String zonedDateTime) {
        // This ensures that a received datetime: 2024-03-05T23:00:00Z is parsed to 2024-03-06T00:00:00+01:00
        return ZonedDateTime.parse(zonedDateTime, DateTimeFormatter.ISO_DATE_TIME)
                            .withZoneSameInstant(DK_ZONE_ID)
                            .toLocalDate();
    }

    @Override
    public MyEnergyDataMarketDocumentResponse payload() {
        return apiResponse();
    }
}
