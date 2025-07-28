package energy.eddie.outbound.rest.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.outbound.rest.RestOutboundBeanConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {
    private final WebConfig webConfig = new WebConfig(new ObjectMapper(),
                                                      new RestOutboundBeanConfig().jaxb2Marshaller());

    @Test
    void configure_removesJacksonMappers_replacesWithFallbackAndCustomJacksonJsonMapper() {
        // Given
        var jacksonConverter = new MappingJackson2HttpMessageConverter();
        var converters = new ArrayList<>(List.of(
                new FormHttpMessageConverter(),
                new MappingJackson2XmlHttpMessageConverter(),
                jacksonConverter
        ));

        // When
        webConfig.configureMessageConverters(converters);

        // Then
        assertThat(converters)
                .hasSize(3)
                .satisfiesExactlyInAnyOrder(
                        item1 -> assertThat(item1).isInstanceOf(FormHttpMessageConverter.class),
                        item2 -> assertThat(item2).isInstanceOf(FallbackXmlMessageConverter.class),
                        item3 -> assertThat(item3)
                                .isNotSameAs(jacksonConverter)
                                .isInstanceOf(MappingJackson2HttpMessageConverter.class)
                );
    }
}