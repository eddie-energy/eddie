package energy.eddie.regionconnector.nl.mijn.aansluiting;

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
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import energy.eddie.regionconnector.shared.agnostic.JsonRawDataProvider;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.ConnectionStatusMessageHandler;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.integration.PermissionMarketDocumentMessageHandler;
import energy.eddie.regionconnector.shared.services.CommonFutureDataService;
import energy.eddie.regionconnector.shared.services.data.needs.DataNeedCalculationServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

import javax.net.ssl.X509KeyManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HashSet;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.NL_ZONE_ID;
import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Configuration
public class MijnAansluitingBeanConfig {

    @Bean
    public PrivateKey privateKey(SslBundles sslBundles) {
        var bundle = sslBundles.getBundle("nl");
        var manager = (X509KeyManager) bundle.getManagers().getKeyManagers()[0];
        return manager.getPrivateKey(bundle.getKey().getAlias());
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
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(new Jdk8Module());
    }

    @Bean
    public DataNeedCalculationService<DataNeed> dataNeedCalculationService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        return new DataNeedCalculationServiceImpl(
                dataNeedsService,
                MijnAansluitingRegionConnectorMetadata.getInstance()
        );
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    @Bean
    @OnRawDataMessagesEnabled
    public RawDataProvider rawDataProvider(ObjectMapper objectMapper, PollingService pollingService) {
        return new JsonRawDataProvider(
                REGION_CONNECTOR_ID,
                objectMapper,
                pollingService.identifiableMeteredDataFlux(),
                pollingService.identifiableAccountingPointDataFlux()
        );
    }

    @Bean
    public ConnectionStatusMessageHandler<MijnAansluitingPermissionRequest> connectionStatusMessageHandler(
            NlPermissionRequestRepository nlPermissionRequestRepository,
            EventBus eventBus
    ) {
        return new ConnectionStatusMessageHandler<>(eventBus, nlPermissionRequestRepository, pr -> "");
    }

    @Bean
    public PermissionMarketDocumentMessageHandler<MijnAansluitingPermissionRequest> permissionMarketDocumentMessageHandler(
            EventBus eventBus,
            NlPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService,
            MijnAansluitingConfiguration config,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig
    ) {
        return new PermissionMarketDocumentMessageHandler<>(
                eventBus,
                repository,
                dataNeedsService,
                config.continuousClientId().getValue(),
                cimConfig,
                pr -> null,
                NL_ZONE_ID
        );
    }

    @Bean
    public CommonFutureDataService<MijnAansluitingPermissionRequest> commonFutureDataService(
            PollingService pollingService,
            NlPermissionRequestRepository repository,
            @Value("${region-connector.nl.mijn.aansluiting.polling:0 0 17 * * *}") String cronExpr,
            MijnAansluitingRegionConnector connector,
            TaskScheduler taskScheduler,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService
    ){
        return new CommonFutureDataService<>(
                pollingService,
                repository,
                cronExpr,
                connector.getMetadata(),
                taskScheduler,
                dataNeedCalculationService
        );
    }
}
