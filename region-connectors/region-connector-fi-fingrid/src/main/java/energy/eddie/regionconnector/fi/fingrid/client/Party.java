package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Party(@JsonProperty("Identification") String identification) {
}
