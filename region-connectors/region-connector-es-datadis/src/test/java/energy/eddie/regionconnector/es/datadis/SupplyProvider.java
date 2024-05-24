package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SupplyProvider {
    public static final ObjectMapper objectMapper = new DatadisSpringConfig().objectMapper();

    public static List<Supply> loadSupply() throws IOException {
        try (InputStream is = SupplyProvider.class.getClassLoader()
                                                  .getResourceAsStream("supply.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }
}
