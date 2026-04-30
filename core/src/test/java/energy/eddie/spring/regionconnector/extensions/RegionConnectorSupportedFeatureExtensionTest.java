// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.MessageStream;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@org.springframework.test.context.TestPropertySource(properties = "eddie.raw.data.output.enabled=true")
@Import(RegionConnectorSupportedFeatureExtensionWithAllBeansPresentTest.TestConfig.class)
class RegionConnectorSupportedFeatureExtensionWithAllBeansPresentTest {
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @MockitoBean
    private RegionConnectorMetadata metadata;
    @MockitoBean
    @SuppressWarnings("unused")
    private RegionConnector regionConnector;
    @MockitoBean
    @SuppressWarnings("unused")
    private RegionConnectorRetransmissionService regionConnectorRetransmissionService;

    @Test
    void testSupportedFeatures_returnsTrue_ifFeatureInApplicationContext() {
        // Given
        var extension = new RegionConnectorSupportedFeatureExtension(new SupportedFeatureService(), metadata);
        extension.setApplicationContext(applicationContext);

        // When & Then
        assertAll(
                () -> assertTrue(extension.supportsConnectionStatusMessages()),
                () -> assertTrue(extension.supportsTermination()),
                () -> assertTrue(extension.supportsRawDataMessages()),
                () -> assertTrue(extension.supportsAccountingPointMarketDocuments()),
                () -> assertTrue(extension.supportsPermissionMarketDocuments()),
                () -> assertTrue(extension.supportsValidatedHistoricalDataMarketDocuments()),
                () -> assertTrue(extension.supportsRetransmissionRequests()),
                () -> assertTrue(extension.supportsValidatedHistoricalDataMarketDocumentsV1_04()),
                () -> assertTrue(extension.supportsNearRealTimeDataMarketDocuments()),
                () -> assertTrue(extension.supportsEnergySharingReferenceDataMarketDocuments())
        );
    }

    @Test
    void testRegionConnectorId_returnsId() {
        // Given
        var extension = new RegionConnectorSupportedFeatureExtension(new SupportedFeatureService(), metadata);
        when(metadata.id()).thenReturn("rc-id");

        // When
        var res = extension.regionConnectorId();

        // Then
        assertEquals("rc-id", res);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        Object streams() {
            return new Object() {
                @MessageStream(ConnectionStatusMessage.class)
                Flux<ConnectionStatusMessage> connectionStatusMessages() {
                    return Flux.empty();
                }

                @MessageStream(RawDataMessage.class)
                Flux<RawDataMessage> rawDataMessages() {
                    return Flux.empty();
                }

                @MessageStream(ValidatedHistoricalDataEnvelope.class)
                Flux<ValidatedHistoricalDataEnvelope> vhdMessages() {
                    return Flux.empty();
                }

                @MessageStream(VHDEnvelope.class)
                Flux<VHDEnvelope> vhdV104Messages() {
                    return Flux.empty();
                }

                @MessageStream(AccountingPointEnvelope.class)
                Flux<AccountingPointEnvelope> apMessages() {
                    return Flux.empty();
                }

                @MessageStream(PermissionEnvelope.class)
                Flux<PermissionEnvelope> permissionMarketDocuments() {
                    return Flux.empty();
                }

                @MessageStream(RTDEnvelope.class)
                Flux<RTDEnvelope> rtdMessages() {
                    return Flux.empty();
                }

                @MessageStream(ESRDMDEnvelope.class)
                Flux<ESRDMDEnvelope> esrMessages() {
                    return Flux.empty();
                }
            };
        }
    }
}

@ExtendWith(SpringExtension.class)
class RegionConnectorSupportedFeatureExtensionWithoutBeansPresentTest {
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @MockitoBean
    private RegionConnectorMetadata metadata;

    @Test
    void testSupportedFeatures_returnsFalse_ifFeatureNotInApplicationContext() {
        // Given
        var extension = new RegionConnectorSupportedFeatureExtension(new SupportedFeatureService(), metadata);
        extension.setApplicationContext(applicationContext);

        // When & Then
        assertAll(
                () -> assertFalse(extension.supportsConnectionStatusMessages()),
                () -> assertFalse(extension.supportsTermination()),
                () -> assertFalse(extension.supportsRawDataMessages()),
                () -> assertFalse(extension.supportsAccountingPointMarketDocuments()),
                () -> assertFalse(extension.supportsPermissionMarketDocuments()),
                () -> assertFalse(extension.supportsValidatedHistoricalDataMarketDocuments()),
                () -> assertFalse(extension.supportsRetransmissionRequests()),
                () -> assertFalse(extension.supportsValidatedHistoricalDataMarketDocumentsV1_04()),
                () -> assertFalse(extension.supportsNearRealTimeDataMarketDocuments()),
                () -> assertFalse(extension.supportsEnergySharingReferenceDataMarketDocuments())
        );
    }
}
