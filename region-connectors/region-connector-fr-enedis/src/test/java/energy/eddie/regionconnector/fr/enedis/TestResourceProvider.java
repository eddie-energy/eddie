package energy.eddie.regionconnector.fr.enedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisDataApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import okhttp3.mockwebserver.MockResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestResourceProvider {
    public static final String CONSUMPTION_LOAD_CURVE_1_DAY = "consumption_load_curve_2024-02-26_to_2024-02-27.json";
    public static final String DAILY_CONSUMPTION_1_WEEK = "daily_consumption_2024-02-01_2024-02-08.json";
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new Jdk8Module());

    public static MeterReading readMeterReadingFromFile(String resource) throws IOException {
        try (InputStream is = TestResourceProvider.class.getClassLoader().getResourceAsStream(resource)) {
            return objectMapper.readValue(Objects.requireNonNull(is), EnedisDataApiResponse.class).meterReading();
        }
    }

    public static MockResponse readMockResponseFromFile(String resource) throws IOException {
        try (InputStream is = TestResourceProvider.class.getClassLoader().getResourceAsStream(resource)) {
            return new MockResponse()
                    .setBody(new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8))
                    .addHeader("Content-Type", "application/json");
        }
    }
}