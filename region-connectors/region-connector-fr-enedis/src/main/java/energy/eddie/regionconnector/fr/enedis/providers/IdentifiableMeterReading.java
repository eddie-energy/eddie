// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.providers;

import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.shared.utils.MeterReadingEndDate;

import java.time.LocalDate;

public record IdentifiableMeterReading(
        FrEnedisPermissionRequest permissionRequest,
        MeterReading payload,
        MeterReadingType meterReadingType

) implements MeterReadingEndDate, IdentifiablePayload<FrEnedisPermissionRequest, MeterReading> {
    @Override
    public LocalDate meterReadingEndDate() {
        return payload.period().end();
    }
}
