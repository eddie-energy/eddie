package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MeteringDataProvider {
    public static final ObjectMapper objectMapper = new DatadisBeanConfig().objectMapper();

    public static List<MeteringData> loadMeteringData() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader().getResourceAsStream("consumptionKWh.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {});
        }
    }

    public static List<MeteringData> loadMeteringDataWithDaylightSavings() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader()
                                                        .getResourceAsStream(
                                                                "consumptionKWh_2023-10-01_to_2024-04-17.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }

    public static List<MeteringData> loadSurplusMeteringData() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader()
                                                        .getResourceAsStream("consumptionKWh-withSurplus.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }
}
