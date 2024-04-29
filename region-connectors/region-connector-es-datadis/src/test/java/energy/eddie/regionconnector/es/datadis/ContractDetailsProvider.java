package energy.eddie.regionconnector.es.datadis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ContractDetailsProvider {
    public static final ObjectMapper objectMapper = new DatadisSpringConfig().objectMapper();

    public static List<ContractDetails> loadContractDetails() throws IOException {
        try (InputStream is = ContractDetailsProvider.class.getClassLoader()
                                                           .getResourceAsStream("contractDetails.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }

    public static List<ContractDetails> loadMultipleContractDetails() throws IOException {
        try (InputStream is = ContractDetailsProvider.class.getClassLoader()
                                                           .getResourceAsStream("contractDetails-multiple.json")) {
            return objectMapper.readValue(is, new TypeReference<>() {
            });
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public static String loadRawContractDetails() throws IOException {
        try (InputStream is = ContractDetailsProvider.class.getClassLoader()
                                                           .getResourceAsStream("contractDetails.json")) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
