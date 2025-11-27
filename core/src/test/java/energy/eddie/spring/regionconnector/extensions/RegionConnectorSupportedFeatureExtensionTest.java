package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.api.v1_04.NearRealTimeDataMarketDocumentProvider;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.core.services.SupportedFeatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RegionConnectorSupportedFeatureExtensionWithAllBeansPresentTest {
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @MockitoBean
    @SuppressWarnings("unused")
    private RegionConnectorMetadata metadata;
    @MockitoBean
    @SuppressWarnings("unused")
    private ConnectionStatusMessageProvider connectionStatusMessageProvider;
    @MockitoBean
    @SuppressWarnings("unused")
    private RawDataProvider rawDataProvider;
    @MockitoBean
    @SuppressWarnings("unused")
    private RegionConnector regionConnector;
    @MockitoBean
    @SuppressWarnings("unused")
    private AccountingPointEnvelopeProvider accountingPointEnvelopeProvider;
    @MockitoBean
    @SuppressWarnings("unused")
    private PermissionMarketDocumentProvider permissionMarketDocumentProvider;
    @MockitoBean
    @SuppressWarnings("unused")
    private ValidatedHistoricalDataEnvelopeProvider validatedHistoricalDataEnvelopeProvider;
    @MockitoBean
    @SuppressWarnings("unused")
    private RegionConnectorRetransmissionService regionConnectorRetransmissionService;
    @MockitoBean
    @SuppressWarnings("unused")
    private ValidatedHistoricalDataMarketDocumentProvider validatedHistoricalDataMarketDocumentProvider;
    @MockitoBean
    @SuppressWarnings("unused")
    private NearRealTimeDataMarketDocumentProvider nearRealTimeDataMarketDocumentProvider;

    @Test
    void testSupportedFeatures_returnsTrue_ifFeatureInApplicationContext() {
        // Given
        var extension = new RegionConnectorSupportedFeatureExtension(new SupportedFeatureService(), metadata);
        extension.setApplicationContext(applicationContext);

        // When & Then
        assertAll(
                () -> assertTrue(extension.supportsConnectionsStatusMessages()),
                () -> assertTrue(extension.supportsTermination()),
                () -> assertTrue(extension.supportsRawDataMessages()),
                () -> assertTrue(extension.supportsAccountingPointMarketDocuments()),
                () -> assertTrue(extension.supportsPermissionMarketDocuments()),
                () -> assertTrue(extension.supportsValidatedHistoricalDataMarketDocuments()),
                () -> assertTrue(extension.supportsRetransmissionRequests()),
                () -> assertTrue(extension.supportsValidatedHistoricalDataMarketDocumentsV1_04()),
                () -> assertTrue(extension.supportsNearRealTimeDataMarketDocuments())
        );
    }

    @Test
    void testSupportedFeature_returnsFalse_withoutContext() {
        // Given
        var extension = new RegionConnectorSupportedFeatureExtension(new SupportedFeatureService(), metadata);

        // When
        var res = extension.supportsValidatedHistoricalDataMarketDocuments();

        // Then
        assertFalse(res);
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
                () -> assertFalse(extension.supportsConnectionsStatusMessages()),
                () -> assertFalse(extension.supportsTermination()),
                () -> assertFalse(extension.supportsRawDataMessages()),
                () -> assertFalse(extension.supportsAccountingPointMarketDocuments()),
                () -> assertFalse(extension.supportsPermissionMarketDocuments()),
                () -> assertFalse(extension.supportsValidatedHistoricalDataMarketDocuments()),
                () -> assertFalse(extension.supportsRetransmissionRequests()),
                () -> assertFalse(extension.supportsValidatedHistoricalDataMarketDocumentsV1_04()),
                () -> assertFalse(extension.supportsNearRealTimeDataMarketDocuments())
        );
    }
}
