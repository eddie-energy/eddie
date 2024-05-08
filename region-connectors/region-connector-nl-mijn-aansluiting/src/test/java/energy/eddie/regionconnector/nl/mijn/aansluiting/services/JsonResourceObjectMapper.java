package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class JsonResourceObjectMapper<T> {
    private final TypeReference<T> model;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public JsonResourceObjectMapper(TypeReference<T> model) {
        this.model = model;
    }

    public T loadTestJson(String fileName) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        return mapper.readValue(inputStream, this.model);
    }

    public String loadRawTestJson(String fileName) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IOException("InputStream null");
        }
        var res = inputStream.readAllBytes();
        inputStream.close();
        return new String(res, Charset.defaultCharset());
    }
}