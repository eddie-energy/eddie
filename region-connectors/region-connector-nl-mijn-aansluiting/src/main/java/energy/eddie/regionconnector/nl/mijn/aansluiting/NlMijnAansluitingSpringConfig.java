package energy.eddie.regionconnector.nl.mijn.aansluiting;

import ch.qos.logback.core.net.ssl.KeyStoreFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import energy.eddie.api.agnostic.RegionConnector;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConsentMarketDocumentMessageHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Sinks;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_FALLBACK_ID_KEY;
import static energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration.ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY;
import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.NL_ZONE_ID;
import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.REGION_CONNECTOR_ID;


@SpringBootApplication
@RegionConnector(name = REGION_CONNECTOR_ID)
@EnableConfigurationProperties(MijnAansluitingConfiguration.class)
public class NlMijnAansluitingSpringConfig {

    @Bean
    public KeyStoreFactoryBean keyStore(MijnAansluitingConfiguration configuration) {
        KeyStoreFactoryBean keyStoreFactoryBean = new KeyStoreFactoryBean();
        keyStoreFactoryBean.setLocation(configuration.keyStoreLocation());
        keyStoreFactoryBean.setPassword(configuration.keyStorePassword());
        keyStoreFactoryBean.setType("JKS");
        return keyStoreFactoryBean;
    }

    @Bean
    public PrivateKey privateKey(MijnAansluitingConfiguration configuration)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, IOException, CertificateException {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(configuration.keyStoreLocation()), configuration.keyStorePassword().toCharArray());
        return (PrivateKey) ks.getKey(configuration.keyAlias(), configuration.keyPassword().toCharArray());
    }

    @Bean
    public OIDCProviderMetadata oidcProviderMetadata(MijnAansluitingConfiguration configuration) throws GeneralException, IOException {
        Issuer issuer = new Issuer(configuration.issuerUrl());
        return OIDCProviderMetadata.resolve(issuer);
    }

    @Bean
    public ConfigurableJWTProcessor<SecurityContext> jwtProcessor(OIDCProviderMetadata metadata) throws MalformedURLException {
        // Create a JWT processor for the access tokens
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        // The public RSA keys to validate the signatures will be sourced from the
        // OAuth 2.0 server's JWK set URL. The key source will cache the retrieved
        // keys for 5 minutes. 30 seconds prior to the cache's expiration the JWK
        // set will be refreshed from the URL on a separate dedicated thread.
        // Retrial is added to mitigate transient network errors.
        JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(metadata.getJWKSetURI().toURL())
                                                               .refreshAheadCache(true)
                                                               .retrying(true)
                                                               .build();


        // The expected JWS algorithm of the access tokens (agreed out-of-band)
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;

        // Configure the JWT processor with a key selector to feed matching public
        // RSA keys sourced from the JWK set URL
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                expectedJWSAlg,
                keySource);
        jwtProcessor.setJWSKeySelector(keySelector);


        // Set the required JWT claims for access tokens
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().issuer(metadata.getIssuer().getValue()).build(),
                new HashSet<>(Arrays.asList(
                        JWTClaimNames.SUBJECT,
                        JWTClaimNames.AUDIENCE,
                        JWTClaimNames.ISSUED_AT,
                        JWTClaimNames.EXPIRATION_TIME,
                        "resources",
                        "consent_id",
                        "scope",
                        JWTClaimNames.JWT_ID))));

        return jwtProcessor;
    }

    @Bean
    public EventBus eventBus() {
        return new EventBusImpl();
    }

    @Bean
    public Outbox outbox(EventBus eventBus, PermissionEventRepository permissionEventRepository) {
        return new Outbox(eventBus, permissionEventRepository);
    }

    @Bean
    public Sinks.Many<ConnectionStatusMessage> connectionStatusMessageSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Sinks.Many<ConsentMarketDocument> consentMarketDocumentSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public CommonInformationModelConfiguration commonInformationModelConfiguration(
            @Value("${" + ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY + "}") String codingSchemeTypeList,
            @Value("${" + ELIGIBLE_PARTY_FALLBACK_ID_KEY + "}") String fallback
    ) {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.fromValue(codingSchemeTypeList),
                                                            fallback);
    }

    @Bean
    public Set<EventHandler<PermissionEvent>> integrationEventHandlers(
            EventBus eventBus,
            NlPermissionRequestRepository repository,
            Sinks.Many<ConnectionStatusMessage> messages,
            Sinks.Many<ConsentMarketDocument> consentMarketDocuments,
            MijnAansluitingConfiguration config,
            CommonInformationModelConfiguration cimConfig
    ) {
        return Set.of(
                new ConnectionStatusMessageHandler<>(eventBus, messages, repository, pr -> ""),
                new ConsentMarketDocumentMessageHandler<>(eventBus,
                                                          repository,
                                                          consentMarketDocuments,
                                                          config.clientId().getValue(),
                                                          cimConfig,
                                                          pr -> null,
                                                          NL_ZONE_ID)
        );
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new Jdk8Module());
    }
}
