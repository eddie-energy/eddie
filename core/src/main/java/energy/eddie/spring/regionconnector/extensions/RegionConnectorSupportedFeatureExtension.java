// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.core.services.SupportedFeatureService;
import jakarta.annotation.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static energy.eddie.core.message.streams.MessageStreamUtils.isProviderMethod;

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
    public boolean supportsConnectionStatusMessages() {
        return hasMessageStream(ConnectionStatusMessage.class);
    }

    @JsonProperty
    public boolean supportsRawDataMessages() {
        return hasMessageStream(RawDataMessage.class);
    }

    @JsonProperty
    public boolean supportsTermination() {
        return hasBean(RegionConnector.class);
    }

    @JsonProperty
    public boolean supportsAccountingPointMarketDocuments() {
        return hasMessageStream(AccountingPointEnvelope.class);
    }

    @JsonProperty
    public boolean supportsPermissionMarketDocuments() {
        return hasMessageStream(PermissionEnvelope.class);
    }

    @JsonProperty
    public boolean supportsValidatedHistoricalDataMarketDocuments() {
        return hasMessageStream(ValidatedHistoricalDataEnvelope.class);
    }

    @JsonProperty
    public boolean supportsRetransmissionRequests() {
        return hasBean(RegionConnectorRetransmissionService.class);
    }

    @JsonProperty
    public boolean supportsNearRealTimeDataMarketDocuments() {
        return hasMessageStream(RTDEnvelope.class);
    }

    @JsonProperty
    @SuppressWarnings("java:S100")
    public boolean supportsNearRealTimeDataMarketDocumentsV1_12() {
        return hasMessageStream(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class);
    }

    @JsonProperty
    @SuppressWarnings("java:S100")
    public boolean supportsValidatedHistoricalDataMarketDocumentsV1_04() {
        return hasMessageStream(VHDEnvelope.class);
    }

    @JsonProperty
    public boolean supportsEnergySharingReferenceDataMarketDocuments() {
        return hasMessageStream(ESRDMDEnvelope.class);
    }

    private boolean hasBean(Class<?> clazz) {
        try {
            context.getBean(clazz);
        } catch (NoSuchBeanDefinitionException e) {
            return false;
        }
        return true;
    }

    private boolean hasMessageStream(Class<?> clazz) {
        for (var beanName : context.getBeanDefinitionNames()) {
            var bean = context.getBean(beanName);
            var targetClass = AopUtils.getTargetClass(bean);
            for (var method : targetClass.getDeclaredMethods()) {
                var annotation = method.getAnnotation(MessageStream.class);
                if (annotation == null || !method.trySetAccessible()) {
                    continue;
                }
                var messageType = annotation.value();

                if (messageType.equals(clazz) && isProviderMethod(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}
