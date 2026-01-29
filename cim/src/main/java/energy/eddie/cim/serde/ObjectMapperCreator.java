package energy.eddie.cim.serde;


import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

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
    public static ObjectMapper create(SerializationFormat format) {
        MapperBuilder<?, ?> builder = switch (format) {
            case XML -> XmlMapper.builder();
            case JSON -> JsonMapper.builder();
        };
        return builder.addModule(new JakartaXmlBindAnnotationModule())
                      .build();
    }
}
