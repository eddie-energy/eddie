package energy.eddie.regionconnector.de.eta.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Configuration
public class EtaPlusClientConfig {

    @Bean(name = "etaWebClient")
    public WebClient etaWebClient(
            WebClient.Builder webClientBuilder,
            PlainDeConfiguration configuration, // Injected configuration
            @Value("${region.connector.de.eta.ssl.enabled:true}") boolean isSslEnabled
    ) throws SSLException {

        HttpClient httpClient = HttpClient.create();

        if (isSslEnabled && configuration.apiBaseUrl().startsWith("https")) {
            SslContext sslContext = SslContextBuilder.forClient().build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }

        Consumer<HttpHeaders> authHeaderConsumer = (HttpHeaders headers) -> {
            headers.setBasicAuth(
                    configuration.apiClientId(),
                    configuration.apiClientSecret(),
                    StandardCharsets.UTF_8
            );
        };

        return webClientBuilder
                .baseUrl(configuration.apiBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(authHeaderConsumer)
                .build();
    }
}