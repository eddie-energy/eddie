package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


@ExtendWith(SpringExtension.class)
@Import(ObjectMapperConfig.class)
@TestPropertySource(properties = "outbound-connector.kafka.format=json")
class ObjectMapperConfigJsonTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper objectMapper;

    @Test
    void testObjectMapper_isJsonObjectMapper() {
        // Given, When, Then
        assertThat(objectMapper, instanceOf(JsonMapper.class));
    }
}

@ExtendWith(SpringExtension.class)
@Import(ObjectMapperConfig.class)
@TestPropertySource(properties = "outbound-connector.kafka.format=xml")
class ObjectMapperConfigXmlTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper objectMapper;

    @Test
    void testObjectMapper_isJsonObjectMapper() {
        // Given, When, Then
        assertThat(objectMapper, instanceOf(XmlMapper.class));
    }
}

@ExtendWith(SpringExtension.class)
@Import(ObjectMapperConfig.class)
class ObjectMapperConfigWithoutPropertyTest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper objectMapper;

    @Test
    void testObjectMapper_isJsonObjectMapper() {
        // Given, When, Then
        assertThat(objectMapper, instanceOf(JsonMapper.class));
    }
}
