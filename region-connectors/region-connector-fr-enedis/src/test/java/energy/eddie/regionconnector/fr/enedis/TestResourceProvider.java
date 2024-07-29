package energy.eddie.regionconnector.fr.enedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import okhttp3.mockwebserver.MockResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestResourceProvider {
    public static final String CONSUMPTION_LOAD_CURVE_1_DAY = "consumption_load_curve_2024-02-26_to_2024-02-27.json";
    public static final String CONSUMPTION_LOAD_CURVE_WITH_CHANGING_RESOLUTION_1_DAY = "consumption_load_curve_with_changing_resolution_2024-02-26_to_2024-02-27.json";
    public static final String DAILY_CONSUMPTION_1_WEEK = "daily_consumption_2024-02-01_2024-02-08.json";
    public static final String CONTRACT = "contract-consumption.json";
    public static final String CONTACT = "contact.json";
    public static final String IDENTITY = "identity.json";
    public static final String IDENTITY_LEGAL_ONLY = "identity-legal-only.json";
    public static final String IDENTITY_NATURAL_ONLY = "identity-natural-only.json";

    public static final String ADDRESS = "address.json";

    private static final ObjectMapper objectMapper = new FrEnedisSpringConfig().objectMapper();

    public static MeterReading readMeterReadingFromFile(String resource) throws IOException {
        try (InputStream is = TestResourceProvider.class.getClassLoader().getResourceAsStream(resource)) {
            return objectMapper.readValue(Objects.requireNonNull(is), MeterReading.class);
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
