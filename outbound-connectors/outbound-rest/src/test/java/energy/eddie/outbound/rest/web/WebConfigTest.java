// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {
    private final JsonMapper jsonMapper = new JsonMapper();
    private final WebConfig webConfig = new WebConfig(new XmlMapper(), jsonMapper);
    @Mock
    private HttpMessageConverters.ServerBuilder serverBuilder;

    @Test
    @SuppressWarnings("ReferenceEquality") // The exact mapper instance must be used.
    void configure_removesJacksonMappers_replacesWithFallbackAndCustomJacksonJsonMapper() {
        // Given

        // When
        webConfig.configureMessageConverters(serverBuilder);

        // Then
        verify(serverBuilder).withXmlConverter(any());
        verify(serverBuilder).withJsonConverter(argThat(
                converter -> converter instanceof JacksonJsonHttpMessageConverter json
                             && json.getMapper() == jsonMapper
        ));
    }
}