package energy.eddie.regionconnector.us.green.button.client.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("unused")
public class MeterList {
    @JsonProperty(value = "meters", required = true)
    private final List<String> meterIds;

    @JsonCreator
    public MeterList(List<String> meterIds) {this.meterIds = meterIds;}
}
