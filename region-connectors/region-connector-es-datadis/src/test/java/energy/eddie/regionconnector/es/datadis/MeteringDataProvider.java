package energy.eddie.regionconnector.es.datadis;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MeteringDataProvider {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
                                                                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                                                                .build();

    public static List<MeteringData> loadMeteringData() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader().getResourceAsStream("consumptionKWh.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {});
        }
    }

    public static List<MeteringData> loadMeteringDataShort() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader()
                                                        .getResourceAsStream("consumptionKWhShort.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {});
        }
    }

    public static List<MeteringData> loadMeteringDataWithDaylightSavings() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader()
                                                        .getResourceAsStream(
                                                                "consumptionKWh_2023-10-01_to_2024-04-17.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {
            });
        }
    }

    public static List<MeteringData> loadSurplusMeteringData() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader()
                                                        .getResourceAsStream("consumptionKWh-withSurplus.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {
            });
        }
    }
}
