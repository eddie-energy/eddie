// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class DtoLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
