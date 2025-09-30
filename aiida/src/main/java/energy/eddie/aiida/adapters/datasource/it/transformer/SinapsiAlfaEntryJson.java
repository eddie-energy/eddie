package energy.eddie.aiida.adapters.datasource.it.transformer;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SinapsiAlfaEntryJson(
    @JsonProperty("du") String meterFabricationNumber,
    @JsonProperty("pod") String pointOfDelivery,
    @JsonProperty("data") List<SinapsiAlfaDataEntryJson> data
) {
    public record SinapsiAlfaDataEntryJson(
            @JsonProperty("ts") float timestamp,
            @JsonIgnore Map<String, Integer> entries
    ) {
        public SinapsiAlfaDataEntryJson {
            entries = new HashMap<>();
        }

        @JsonAnySetter
        void capture(String key, Integer value) {
            entries.put(key, value);
        }
    }
}
