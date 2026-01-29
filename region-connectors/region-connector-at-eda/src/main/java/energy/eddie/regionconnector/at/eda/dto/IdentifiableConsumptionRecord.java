// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;
import java.util.List;

public record IdentifiableConsumptionRecord(
        EdaConsumptionRecord consumptionRecord,
        List<AtPermissionRequest> permissionRequests,
        LocalDate meterReadingStartDate,
        LocalDate meterReadingEndDate
) implements MeterReadingEndDate {
}
