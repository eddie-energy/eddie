// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * This class represents the metering data returned by the Datadis API. It contains both consumption and surplus energy
 * (production) data.
 *
 * @param cups             the CUPS identifier (metering point identifier)
 * @param date             the date of the metering data
 * @param time             the time of the metering data in the format "HH:mm"
 * @param consumptionKWh   the consumption in kWh (always present)
 * @param obtainMethod     the method used to obtain the metering data {@link ObtainMethod}
 * @param surplusEnergyKWh the surplus energy in kWh. The default value is 0, this can't be used to determine if the
 *                         metering data contains surplus energy, as Datadis does not define if a DSO is required to
 *                         return surplus energy data if it is 0
 */
public record MeteringData(
        @JsonProperty(value = "cups")
        String cups,
        @JsonProperty(value = "date")
        @JsonFormat(pattern = "yyyy/MM/dd")
        LocalDate date,
        @JsonProperty(value = "time")
        String time,
        @JsonProperty(value = "consumptionKWh")
        double consumptionKWh,
        @JsonProperty(value = "obtainMethod")
        ObtainMethod obtainMethod,
        @JsonProperty(value = "surplusEnergyKWh", defaultValue = "0")
        double surplusEnergyKWh
) {
}
