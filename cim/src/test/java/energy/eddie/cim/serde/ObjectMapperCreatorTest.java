package energy.eddie.cim.serde;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ObjectMapperCreatorTest {

    public static Stream<Arguments> testCreate_createsMapperForFormat() {
        return Stream.of(
                Arguments.of(SerializationFormat.JSON, JsonMapper.class),
                Arguments.of(SerializationFormat.XML, XmlMapper.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCreate_createsMapperForFormat(SerializationFormat format, Class<?> clazz) {
        // Given
        // When
        var mapper = ObjectMapperCreator.create(format);

        // Then
        assertInstanceOf(clazz, mapper);
    }
}