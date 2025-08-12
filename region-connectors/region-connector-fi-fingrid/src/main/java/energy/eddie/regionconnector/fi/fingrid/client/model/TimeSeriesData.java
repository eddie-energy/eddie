package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TimeSeriesData(
        @JsonProperty("Header") Header header,
        @JsonProperty("Transaction") TimeSeriesTransaction transaction
) {}
