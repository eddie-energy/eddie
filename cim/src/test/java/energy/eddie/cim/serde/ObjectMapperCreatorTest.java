// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

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