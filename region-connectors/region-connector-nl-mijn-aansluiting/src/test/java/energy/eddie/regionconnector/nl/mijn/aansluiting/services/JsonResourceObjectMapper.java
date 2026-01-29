// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class JsonResourceObjectMapper<T> {
    private final TypeReference<T> model;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonResourceObjectMapper(TypeReference<T> model) {
        this.model = model;
    }

    public T loadTestJson(String fileName) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        return mapper.readValue(inputStream, this.model);
    }

    public static String loadRawTestJson(String fileName) throws IOException {
        ClassLoader classLoader = JsonResourceObjectMapper.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IOException("InputStream null");
        }
        var res = inputStream.readAllBytes();
        inputStream.close();
        return new String(res, Charset.defaultCharset());
    }
}