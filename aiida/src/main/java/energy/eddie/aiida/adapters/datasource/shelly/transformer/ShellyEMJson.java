package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public record ShellyEMJson(
        @JsonProperty("src") String source,
        @JsonProperty("dst") String destination,
        @JsonProperty("method") String method,
        @JsonProperty("params") Params params
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellyEMJson.class);

    public record Params(
            @JsonProperty("ts") Double timestamp,
            @JsonIgnore EnumMap<ShellyEMComponent, Map<String, Number>> em
    ) {
        public Params {
            em = new EnumMap<>(ShellyEMComponent.class);
        }

        @JsonAnySetter
        void capture(String key, Object value) {
            var component = ShellyEMComponent.fromKey(key);

            if(component == ShellyEMComponent.UNKNOWN) {
                LOGGER.trace("Ignoring unknown component key: {}", key);
                return;
            }

            if (value instanceof Map<?, ?> map) {
                Map<String, Number> values = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String strKey && entry.getValue() instanceof Number numValue) {
                        values.put(strKey, numValue);
                    }
                }
                em.put(component, values);
            }
        }
    }
}
