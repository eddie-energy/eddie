package energy.eddie.regionconnector.de.eta.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EtaPlusAccountingPointData(
        String meteringPointId,
        @Nullable @JsonProperty("customerId") String customerId,
        @Nullable @JsonProperty("streetName") String streetName,
        @Nullable @JsonProperty("address") String address, // Keep for backward compatibility
        @Nullable @JsonProperty("postalCode") String postalCode,
        @Nullable @JsonProperty("city") String city,
        @Nullable @JsonProperty("country") String country,
        @Nullable @JsonProperty("energyType") String energyType,
        @Nullable @JsonProperty("voltageLevel") String voltageLevel,
        @Nullable @JsonProperty("connectionDate") LocalDate connectionDate,
        @Nullable @JsonProperty("status") String status,
        String rawJson
) {
    /**
     * Gets the street name, falling back to address if streetName is null.
     */
    @Nullable
    public String streetName() {
        return streetName != null ? streetName : address;
    }
    /**
     * Creates an EtaPlusAccountingPointData with raw JSON for raw data emission.
     */
    public static EtaPlusAccountingPointData withRawJson(
            String meteringPointId,
            @Nullable String customerId,
            @Nullable String streetName,
            @Nullable String address,
            @Nullable String postalCode,
            @Nullable String city,
            @Nullable String country,
            @Nullable String energyType,
            @Nullable String voltageLevel,
            @Nullable LocalDate connectionDate,
            @Nullable String status,
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
