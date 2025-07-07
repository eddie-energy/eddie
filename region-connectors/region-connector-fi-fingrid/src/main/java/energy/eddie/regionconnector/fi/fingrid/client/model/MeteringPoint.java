package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeteringPoint(
        @JsonProperty(value = "MeteringPointEAN", required = true) String meteringPointEAN,
        @JsonProperty("MeteringPointStatus") String meteringPointStatus,
        @JsonProperty(value = "MeteringPointAddress", required = true) MeteringPointAddress meteringPointAddress

) {
}
