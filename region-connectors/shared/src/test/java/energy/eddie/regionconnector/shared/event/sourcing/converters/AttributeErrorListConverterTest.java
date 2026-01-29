// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing.converters;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttributeErrorListConverterTest {
    @Test
    void testConvertToDatabaseColumn() {
        // Given
        List<AttributeError> attributeErrors = List.of(new AttributeError("name", "error message"));
        var attributeErrorListConverter = new AttributeErrorListConverter();
        // When
        String result = attributeErrorListConverter.convertToDatabaseColumn(attributeErrors);

        // Then
        assertThat(result).isEqualTo("[{\"name\":\"name\",\"message\":\"error message\"}]");
    }

    @Test
    void testConvertToEntityAttribute() {
        // Given
        String value = "[{\"name\": \"name\", \"message\":\"error message\"}]";
        var attributeErrorListConverter = new AttributeErrorListConverter();

        // When
        List<AttributeError> result = attributeErrorListConverter.convertToEntityAttribute(value);

        // Then
        assertThat(result).isEqualTo(List.of(new AttributeError("name", "error message")));
    }

    @Test
    void testConvertToEntityAttribute_withInvalidJson() {
        // Given
        String value = "[{\"blablabla\": \"name\", \"asdf\":\"error message\"}]";
        var attributeErrorListConverter = new AttributeErrorListConverter();

        // When
        // Then
        assertThrows(AttributeErrorListConverterException.class,
                     () -> attributeErrorListConverter.convertToEntityAttribute(value));
    }
}