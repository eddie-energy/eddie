package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TimeSeriesResponse(@JsonProperty("RetrieveTimeSeriesData") TimeSeriesData data) {
}
