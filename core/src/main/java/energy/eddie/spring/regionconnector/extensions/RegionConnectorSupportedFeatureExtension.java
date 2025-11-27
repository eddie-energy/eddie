package energy.eddie.spring.regionconnector.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.api.v1_04.NearRealTimeDataMarketDocumentProvider;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.core.services.SupportedFeatureService;
import jakarta.annotation.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@RegionConnectorExtension
public class RegionConnectorSupportedFeatureExtension implements ApplicationContextAware {
    private final RegionConnectorMetadata metadata;
    @Nullable
    private ApplicationContext context;

    public RegionConnectorSupportedFeatureExtension(
            SupportedFeatureService supportedFeatureService,
            RegionConnectorMetadata regionConnectorMetadata
    ) {
        this.metadata = regionConnectorMetadata;
        supportedFeatureService.register(this);
    }

    @Override
    public void setApplicationContext(@SuppressWarnings("NullableProblems") ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @JsonProperty
    public String regionConnectorId() {
        return metadata.id();
    }

    @JsonProperty
    public boolean supportsConnectionsStatusMessages() {
        return hasBean(ConnectionStatusMessageProvider.class);
    }

    @JsonProperty
    public boolean supportsRawDataMessages() {
        return hasBean(RawDataProvider.class);
    }

    @JsonProperty
    public boolean supportsTermination() {
        return hasBean(RegionConnector.class);
    }

    @JsonProperty
    public boolean supportsAccountingPointMarketDocuments() {
        return hasBean(AccountingPointEnvelopeProvider.class);
    }

    @JsonProperty
    public boolean supportsPermissionMarketDocuments() {
        return hasBean(PermissionMarketDocumentProvider.class);
    }

    @JsonProperty
    public boolean supportsValidatedHistoricalDataMarketDocuments() {
        return hasBean(ValidatedHistoricalDataEnvelopeProvider.class);
    }

    @JsonProperty
    public boolean supportsRetransmissionRequests() {
        return hasBean(RegionConnectorRetransmissionService.class);
    }

    @JsonProperty
    public boolean supportsNearRealTimeDataMarketDocuments() {
        return hasBean(NearRealTimeDataMarketDocumentProvider.class);
    }

    @JsonProperty
    @SuppressWarnings("java:S100")
    public boolean supportsValidatedHistoricalDataMarketDocumentsV1_04() {
        return hasBean(ValidatedHistoricalDataMarketDocumentProvider.class);
    }

    private boolean hasBean(Class<?> clazz) {
        if (context == null) {
            return false;
        }
        try {
            context.getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {
            return false;
        }
        return true;
    }
}
