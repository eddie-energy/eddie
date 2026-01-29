package energy.eddie.regionconnector.de.eta.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EtaPlusAccountingPointData(
        String meteringPointId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("streetName") String streetName,
        @JsonProperty("address") String address, // Keep for backward compatibility
        @JsonProperty("postalCode") String postalCode,
        @JsonProperty("city") String city,
        @JsonProperty("country") String country,
        @JsonProperty("energyType") String energyType,
        @JsonProperty("voltageLevel") String voltageLevel,
        @JsonProperty("connectionDate") LocalDate connectionDate,
        @JsonProperty("status") String status,
        String rawJson
) {
    /**
     * Gets the street name, falling back to address if streetName is null.
     */
    public String streetName() {
        return streetName != null ? streetName : address;
    }
    /**
     * Creates an EtaPlusAccountingPointData with raw JSON for raw data emission.
     */
    public static EtaPlusAccountingPointData withRawJson(
            String meteringPointId,
            String customerId,
            String streetName,
            String address,
            String postalCode,
            String city,
            String country,
            String energyType,
            String voltageLevel,
            LocalDate connectionDate,
            String status,
            String rawJson
    ) {
        return new EtaPlusAccountingPointData(
                meteringPointId,
                customerId,
                streetName,
                address,
                postalCode,
                city,
                country,
                energyType,
                voltageLevel,
                connectionDate,
                status,
                rawJson
        );
    }
}
