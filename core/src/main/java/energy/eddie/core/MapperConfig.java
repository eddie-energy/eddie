package energy.eddie.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.core.dtos.SimpleDataSourceInformation;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MapperConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.deserializerByType(DataSourceInformation.class,
                                                     new DataSourceInformationDeserializer());
    }

    private static class DataSourceInformationDeserializer extends StdDeserializer<DataSourceInformation> {

        public DataSourceInformationDeserializer() {
            this(DataSourceInformation.class);
        }

        public DataSourceInformationDeserializer(final Class<?> vc) {
            super(vc);
        }

        @Override
        public DataSourceInformation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final JsonNode playerNode = mapper.readTree(jsonParser);
            return mapper.treeToValue(playerNode, SimpleDataSourceInformation.class);
        }
    }
}
