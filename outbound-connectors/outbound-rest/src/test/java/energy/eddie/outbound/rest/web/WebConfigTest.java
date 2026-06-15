// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageConverters;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebConfigTest {
    private final WebConfig webConfig = new WebConfig(new XmlMapper());
    @Mock
    private HttpMessageConverters.ServerBuilder serverBuilder;

    @Test
    void configure_removesJacksonMappers_replacesWithFallbackAndCustomJacksonJsonMapper() {
        // Given

        // When
        webConfig.configureMessageConverters(serverBuilder);

        // Then
        verify(serverBuilder).withXmlConverter(any());
    }
}