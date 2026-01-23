// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.dataformat.xml.XmlMapper;

@Configuration
@EnableWebMvc
@Primary
public class WebConfig implements WebMvcConfigurer {
    private final XmlMapper xmlMapper;
    private final Jaxb2Marshaller marshaller;

    public WebConfig(XmlMapper xmlMapper, Jaxb2Marshaller marshaller) {
        this.xmlMapper = xmlMapper;
        this.marshaller = marshaller;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        var fallbackConverter = new FallbackXmlMessageConverter(new MarshallingHttpMessageConverter(marshaller),
                                                                new JacksonXmlHttpMessageConverter(xmlMapper));
        builder.withXmlConverter(fallbackConverter);
    }
}
