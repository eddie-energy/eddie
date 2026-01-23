// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SupplyProvider {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Supply> loadSupply() throws IOException {
        try (InputStream is = SupplyProvider.class.getClassLoader()
                                                  .getResourceAsStream("supply.json")) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<>() {
            });
        }
    }
}
