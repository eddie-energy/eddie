// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * This class represents the contract details returned by the Datadis API. The api documentation can be found <a
 * href="https://datadis.es/private-api">here</a>.
 *
 * @param cups                    CUPS identifier (metering point identifier)
 * @param distributor             Distributor
 * @param marketer                Retailer (only returned if the user launching the query is the owner of the CUPS)
 * @param tension                 Voltage
 * @param accessFare              Access tariff description
 * @param province                Province
 * @param municipality            Municipality
 * @param postalCode              Postal code
 * @param contractedPowerkW       Contracted power in kW
 * @param timeDiscrimination      Time discrimination
 * @param modePowerControl        Power control mode (ICP/Maxímetro)
 * @param startDate               Contract start date
 * @param endDate                 Contract end date
 * @param codeFare                Access tariff code (CNMC codes)
 * @param selfConsumptionTypeCode Self-consumption type code (CNMC codes). Only present if there is self-consumption.
 * @param selfConsumptionTypeDesc Description of the self-consumption type. Only present if there is self-consumption.
 * @param section                 Section. Only present if there is self-consumption.
 * @param subsection              Subsection. Only present if there is self-consumption.
 * @param partitionCoefficient    Distribution coefficient. Only present if there is self-consumption.
 * @param cau                     CAU. Only present if there is self-consumption.
 * @param installedCapacity       Installed generation capacity. Only present if there is self-consumption.
 */
public record ContractDetails(
        @JsonProperty("cups")
        String cups,
        @JsonProperty("distributor")
        String distributor,
        @JsonProperty("marketer")
        String marketer,
        @JsonProperty("tension")
        String tension,
        @JsonProperty("accessFare")
        String accessFare,
        @JsonProperty("province")
        String province,
        @JsonProperty("municipality")
        String municipality,
        @JsonProperty("postalCode")
        String postalCode,
        @JsonProperty("contractedPowerkW")
        List<Double> contractedPowerkW,
        @JsonProperty("timeDiscrimination")
        String timeDiscrimination,
        @JsonProperty("modePowerControl")
        String modePowerControl,
        @JsonProperty("startDate")
        @JsonFormat(pattern = "yyyy/MM/dd")
        LocalDate startDate,
        @JsonProperty("endDate")
        @JsonFormat(pattern = "yyyy/MM/dd")
        Optional<LocalDate> endDate,
        @JsonProperty("codeFare")
        String codeFare,
        @JsonProperty("selfConsumptionTypeCode")
        Optional<String> selfConsumptionTypeCode,
        @JsonProperty("selfConsumptionTypeDesc")
        Optional<String> selfConsumptionTypeDesc,
        @JsonProperty("section")
        Optional<String> section,
        @JsonProperty("subsection")
        Optional<String> subsection,
        @JsonProperty("partitionCoefficient")
        Optional<String> partitionCoefficient,
        @JsonProperty("cau")
        Optional<String> cau,
        @JsonProperty("installedCapacity")
        Optional<String> installedCapacity
) {

}
