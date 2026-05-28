// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config;

import energy.eddie.aiida.adapters.datasource.at.transformer.OesterreichsEnergieAdapterJson;
import energy.eddie.aiida.adapters.datasource.at.transformer.OesterreichsEnergieAdapterValueDeserializer;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataField;
import energy.eddie.aiida.adapters.datasource.fr.transformer.MicroTeleinfoV3DataFieldDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.datatype.hibernate7.Hibernate7Module;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

@Configuration
@EnableScheduling
public class AiidaConfiguration {
    public static final ZoneId AIIDA_ZONE_ID = ZoneId.of("Etc/UTC");

    /**
     * Configures and returns an ObjectMapper bean that should be used for (de-)serializing POJOs to JSON. The
     * ObjectMapperSingleton can also be used by classes that cannot use constructor injection using the @Autowired
     * annotation and will return the same instance.
     *
     * @return ObjectMapper instance configured to fit the AIIDA project.
     */
    @Bean
    @Primary
    public JsonMapperBuilderCustomizer objectMapperCustomizer() {
        return builder -> builder
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .addModules(
                        new JakartaXmlBindAnnotationModule(),
                        new Hibernate7Module().enable(Hibernate7Module.Feature.FORCE_LAZY_LOADING)
                                              .disable(Hibernate7Module.Feature.USE_TRANSIENT_ANNOTATION),
                        new SimpleModule()
                                .addDeserializer(OesterreichsEnergieAdapterJson.AdapterValue.class,
                                                 new OesterreichsEnergieAdapterValueDeserializer()),
                        new SimpleModule()
                                .addDeserializer(MicroTeleinfoV3DataField.class,
                                                 new MicroTeleinfoV3DataFieldDeserializer())
                );
    }

    /**
     * Returns a clock instance that should be used for timestamps (e.g. when a permission is revoked).
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Alternative {@link WebClient} to use for the handshake with EDDIE if a localhost replacement is set.
     * For handshake URLs with "localhost" as hostname, the hostname will be replaced with the replacement string.
     * This is useful if handshake URLs pointing to "localhost" are to be interpreted as an EDDIE instance on the same host.
     *
     * @param replacementHost Host to replace "localhost" with.
     */
    @Bean(name = "webClient")
    @ConditionalOnProperty(prefix = "aiida.handshake", name = "localhost-replacement")
    public WebClient webClientWithLocalhostReplacement(
            @Value("${aiida.handshake.localhost-replacement}") String replacementHost
    ) {
        return WebClient.builder().filter(localhostReplacementFilter(replacementHost)).build();
    }

    /**
     * {@link WebClient} used for handshake with EDDIE.
     */
    @Bean
    @ConditionalOnMissingBean(name = "webClient")
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    // Spring's TaskScheduler only returns a ScheduledFuture<?>, so we have to use wildcards
    @SuppressWarnings("java:S1452")
    public ConcurrentMap<UUID, ScheduledFuture<?>> permissionFutures() {
        return new ConcurrentHashMap<>();
    }

    protected static ExchangeFilterFunction localhostReplacementFilter(String replacementHost) {
        return (request, next) -> {
            if ("localhost".equalsIgnoreCase(request.url().getHost())) {
                var url = UriComponentsBuilder.fromUri(request.url())
                                              .host(replacementHost)
                                              .build()
                                              .toUri();

                var mutated = ClientRequest.from(request).url(url).build();
                return next.exchange(mutated);
            }

            return next.exchange(request);
        };
    }
}
