package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MeteringDataProvider {
    public static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    public static List<MeteringData> loadMeteringData() throws IOException {
        try (InputStream is = MeteringDataProvider.class.getClassLoader().getResourceAsStream("consumptionKWh.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }
}
