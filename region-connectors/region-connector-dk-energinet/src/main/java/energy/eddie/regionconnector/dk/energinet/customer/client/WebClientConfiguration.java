package energy.eddie.regionconnector.dk.energinet.customer.client;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class WebClientConfiguration {
    @Bean
    public WebClient webClient(JsonMapper mapper) {
        return WebClient.builder()
                        .codecs(codecs -> {
                            codecs.defaultCodecs()
                                  .jacksonJsonEncoder(new JacksonJsonEncoder(mapper, MediaType.APPLICATION_JSON));
                            codecs.defaultCodecs().jacksonJsonDecoder(
                                    new JacksonJsonDecoder(mapper, MediaType.APPLICATION_JSON)
                            );
                        })
                        .build();
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        // TODO: JsonNullableModule
        return builder -> builder
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
