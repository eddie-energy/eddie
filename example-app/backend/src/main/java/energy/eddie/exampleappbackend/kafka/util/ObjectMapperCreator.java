package energy.eddie.exampleappbackend.kafka.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

class ObjectMapperCreator {
    private ObjectMapperCreator() {
        // Utility Class
    }

    /**
     * Creates an object mapper instance that's specific for the given format.
     *
     * @param format The format that the object mapper should be able to serialize and deserialize.
     * @return object mapper instance for given format.
     */
    public static ObjectMapper create(Format format) {
        return get(format)
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JakartaXmlBindAnnotationModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static ObjectMapper get(Format format) {
        return switch (format) {
            case XML -> new XmlMapper();
            case JSON -> new JsonMapper();
        };
    }
}
