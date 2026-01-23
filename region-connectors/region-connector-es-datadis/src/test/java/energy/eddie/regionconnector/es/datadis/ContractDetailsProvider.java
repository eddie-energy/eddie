// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ContractDetailsProvider {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<ContractDetails> loadContractDetails() throws IOException {
        try (InputStream is = ContractDetailsProvider.class.getClassLoader()
                                                           .getResourceAsStream("contractDetails.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {
            });
        }
    }

    public static List<ContractDetails> loadMultipleContractDetails() throws IOException {
        try (InputStream is = ContractDetailsProvider.class.getClassLoader()
                                                           .getResourceAsStream("contractDetails-multiple.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {
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
