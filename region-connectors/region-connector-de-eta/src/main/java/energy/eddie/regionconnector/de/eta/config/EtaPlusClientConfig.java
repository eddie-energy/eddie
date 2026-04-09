package energy.eddie.regionconnector.de.eta.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.json.JsonMapper;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class EtaPlusClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtaPlusClientConfig.class);

    @Bean(name = "etaWebClient")
    public WebClient etaWebClient(
            WebClient.Builder webClientBuilder,
            DeEtaPlusConfiguration configuration
    ) throws SSLException {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(configuration.responseTimeoutSeconds()));

        if (configuration.sslTrustAll()) {
            LOGGER.warn("SSL certificate validation is DISABLED for ETA+ API requests. Do not use in production!");
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        } else if (configuration.sslEnabled()) {
            SslContext sslContext = SslContextBuilder.forClient().build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }

        return webClientBuilder
                .baseUrl(configuration.apiBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs()
                        .jacksonJsonDecoder(new JacksonJsonDecoder(new JsonMapper(), MediaType.APPLICATION_JSON)))
                .build();
    }
}
