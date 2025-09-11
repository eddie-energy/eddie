package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public record ShellyJson(
        @JsonProperty("src") String source,
        @JsonProperty("dst") String destination,
        @JsonProperty("method") String method,
        @JsonProperty("params") Params params
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShellyJson.class);
    private static final String NESTED_KEY_SEPARATOR = ".";

    public record Params(
            @JsonProperty("ts") Double timestamp,
            @JsonIgnore EnumMap<ShellyComponent, Map<String, Number>> em
    ) {
        public Params {
            em = new EnumMap<>(ShellyComponent.class);
        }

        @JsonAnySetter
        void capture(String key, Object value) {
            var component = ShellyComponent.fromKey(key);

            if (component == ShellyComponent.UNKNOWN) {
                LOGGER.trace("Ignoring unknown component key: {}", key);
                return;
            }

            if (value instanceof Map<?, ?> map) {
                Map<String, Number> values = new HashMap<>();
                flattenMap("", map, values);
                em.put(component, values);
            }
        }

        /**
         * Recursively flattens nested maps into dot-separated keys.
         */
        private void flattenMap(String prefix, Map<?, ?> map, Map<String, Number> values) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String strKey)) {
                    continue;
                }

                String fullKey = prefix.isEmpty() ? strKey : prefix + NESTED_KEY_SEPARATOR + strKey;

                Object val = entry.getValue();
                if (val instanceof Number num) {
                    values.put(fullKey, num);
                } else if (val instanceof Map<?, ?> nested) {
                    flattenMap(fullKey, nested, values);
                }
            }
        }
    }
}
