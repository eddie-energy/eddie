package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the JSON that is received from the Oesterreichs Energie Smart Meter Adapter.
 *
 * @param energyData Holds the values from the smart meter adapter. Keys are the OBIS codes and the value and timestamp are stored in the values.
 * @param apiVersion API version of the smart meter adapter.
 * @param name       Name as entered during the smart meter adapter setup.
 * @param smaTime    Indicates the time of the smart meter <b>adapter</b>.
 */
public record OesterreichAdapterJson(
        // this map is serialized as flattened map by using @JsonAnySetter and @JsonAnyGetter
        @JsonIgnore
        Map<String, AdapterValue> energyData,
        @JsonProperty("api_version")
        String apiVersion,
        @JsonProperty("name")
        String name,
        @JsonProperty("sma_time")
        Double smaTime) {
    public OesterreichAdapterJson {
        energyData = new HashMap<>();
    }

    @JsonAnySetter
    public void putEnergyData(String key, AdapterValue value) {
        energyData.put(key, value);
    }

    public record AdapterValue(
            // use Object type to support Strings, Numbers, etc...
            @JsonProperty("value")
            Object value,
            @JsonProperty("time")
            Long time) {
    }
}
