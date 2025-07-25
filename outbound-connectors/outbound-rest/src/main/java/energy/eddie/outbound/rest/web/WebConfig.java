package energy.eddie.outbound.rest.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebMvc
@Primary
public class WebConfig implements WebMvcConfigurer {
    private final ObjectMapper objectMapper;
    private final Jaxb2Marshaller marshaller;

    public WebConfig(@Qualifier("objectMapper") ObjectMapper objectMapper, Jaxb2Marshaller marshaller) {
        this.objectMapper = objectMapper;
        this.marshaller = marshaller;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        var xmlConverter = converters.stream()
                                     .filter(MappingJackson2XmlHttpMessageConverter.class::isInstance)
                                     .map(MappingJackson2XmlHttpMessageConverter.class::cast)
                                     .findFirst()
                                     .orElseGet(MappingJackson2XmlHttpMessageConverter::new);
        converters.removeIf(MappingJackson2XmlHttpMessageConverter.class::isInstance);
        var fallbackConverter = new FallbackXmlMessageConverter(new MarshallingHttpMessageConverter(marshaller),
                                                                xmlConverter);
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        converters.add(fallbackConverter);
    }
}
