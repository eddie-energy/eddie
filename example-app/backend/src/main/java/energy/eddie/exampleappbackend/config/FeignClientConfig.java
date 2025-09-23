package energy.eddie.exampleappbackend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class FeignClientConfig {
    @Bean
    public Decoder feignDecoder() {
        var jacksonConverter = new MappingJackson2HttpMessageConverter(feignObjectMapper());
        var httpMessageConverters = new HttpMessageConverters(jacksonConverter);
        ObjectFactory<HttpMessageConverters> objectFactory = () -> httpMessageConverters;
        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }

    public ObjectMapper feignObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JakartaXmlBindAnnotationModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
