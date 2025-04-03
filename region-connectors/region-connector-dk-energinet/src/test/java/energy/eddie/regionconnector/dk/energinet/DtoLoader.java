package energy.eddie.regionconnector.dk.energinet;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;

import java.io.IOException;
import java.io.InputStream;

public class DtoLoader {
    private static final ObjectMapper MAPPER = new EnerginetBeanConfig().objectMapper();

    public static MeteringPointDetailsCustomerDtoResponseListApiResponse validApiResponse() throws IOException {
        try (InputStream is = DtoLoader.class.getClassLoader()
                                             .getResourceAsStream(
                                                     "MeteringPointDetailsCustomerDtoResponseListApiResponse.json")) {
            return MAPPER.readValue(is, MeteringPointDetailsCustomerDtoResponseListApiResponse.class);
        }
    }
}
