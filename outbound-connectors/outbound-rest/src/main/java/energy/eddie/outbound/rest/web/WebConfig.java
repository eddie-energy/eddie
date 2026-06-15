// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.dataformat.xml.XmlMapper;

@Configuration
@EnableWebMvc
@Primary
public class WebConfig implements WebMvcConfigurer {
    private final XmlMapper xmlMapper;

    public WebConfig(XmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        var fallbackConverter = new FallbackXmlMessageConverter(new JacksonXmlHttpMessageConverter(xmlMapper));
        builder.withXmlConverter(fallbackConverter);
    }
}
