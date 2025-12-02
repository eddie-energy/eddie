package energy.eddie.regionconnector.dk.energinet;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;

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

    public static MyEnergyDataMarketDocumentResponseListApiResponse loadValidatedHistoricalData() throws IOException {
        var classLoader = DtoLoader.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(
                "ShortMyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            return MAPPER.readValue(is, MyEnergyDataMarketDocumentResponseListApiResponse.class);
        }
    }
}
