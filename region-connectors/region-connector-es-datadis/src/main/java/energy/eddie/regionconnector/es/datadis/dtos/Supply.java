package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

/**
 * This class represents the supply data returned by the Datadis API.
 * It contains information about supplies (metering points) associated to a NIF.
 *
 * @param address The address of the supply.
 * @param meteringPoint Also called cups, the ID of the metering point.
 * @param postalCode The postal code of the supply.
 * @param province The province of the supply.
 * @param municipality The municipality of the supply.
 * @param distributor The distributor for the supply.
 * @param validDateFrom The start date from which the supply is valid.
 * @param validDateTo The end date to which the supply is valid. Might be null.
 * @param pointType is the type of the metering point.
 *                  Values 1 and 2 support quarter hourly values and 3 - 5 support only hourly values.
 * @param distributorCode The code of the supply distributor.
 */
public record Supply(String address,
                     @JsonProperty("cups") String meteringPoint,
                     String postalCode,
                     String province,
                     String municipality,
                     String distributor,
                     @JsonFormat(pattern = "yyyy/MM/dd") LocalDate validDateFrom,
                     @Nullable @JsonFormat(pattern = "yyyy/MM/dd") LocalDate validDateTo,
                     Integer pointType,
                     String distributorCode) {
}
