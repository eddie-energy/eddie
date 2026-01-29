package energy.eddie.regionconnector.fi.fingrid;

import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Objects;

public class TestResourceProvider {
    public static final String TIME_SERIES_WITH_ERRORS = "timeSeriesWithErrors.json";
    public static final String TIME_SERIES_WITH_VALUES = "timeSeriesWithValues.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String CUSTOMER_DATA_JSON = "customerData.json";
    public static final String EMPTY_CUSTOMER_DATA_JSON = "customerDataWithoutData.json";

    public static TimeSeriesResponse readTimeSeriesFromFile(String resource) {
        try (InputStream is = TestResourceProvider.class.getClassLoader().getResourceAsStream(resource)) {
            return OBJECT_MAPPER.readValue(Objects.requireNonNull(is), TimeSeriesResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CustomerDataResponse readCustomerDataFromFile(String resource) {
        try (InputStream is = TestResourceProvider.class.getClassLoader().getResourceAsStream(resource)) {
            return OBJECT_MAPPER.readValue(Objects.requireNonNull(is), CustomerDataResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
