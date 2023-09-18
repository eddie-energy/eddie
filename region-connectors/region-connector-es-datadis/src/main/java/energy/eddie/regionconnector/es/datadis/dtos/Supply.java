package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

/**
 * This class represents the supply data returned by the Datadis API.
 * It contains information about supplies (metering points) associated to a NIF.
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
