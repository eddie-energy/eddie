package energy.eddie.regionconnector.fi.fingrid;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.fi.fingrid.client.TimeSeriesResponse;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class TestResourceProvider {
    public static final String TIME_SERIES_WITH_ERRORS = "timeSeriesWithErrors.json";
    public static final String TIME_SERIES_WITH_VALUES = "timeSeriesWithValues.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();

    public static TimeSeriesResponse readTimeSeriesFromFile(String resource) throws IOException {
        try (InputStream is = TestResourceProvider.class.getClassLoader().getResourceAsStream(resource)) {
            return OBJECT_MAPPER.readValue(Objects.requireNonNull(is), TimeSeriesResponse.class);
        }
    }
}
