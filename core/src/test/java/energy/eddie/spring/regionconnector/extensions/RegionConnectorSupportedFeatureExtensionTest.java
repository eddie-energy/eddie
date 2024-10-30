package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.core.services.SupportedFeatureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class RegionConnectorSupportedFeatureExtensionWithAllBeansPresentTest {
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    @MockBean
    @SuppressWarnings("unused")
    private RegionConnectorMetadata metadata;
    @MockBean
    @SuppressWarnings("unused")
    private ConnectionStatusMessageProvider connectionStatusMessageProvider;
    @MockBean
    @SuppressWarnings("unused")
    private RawDataProvider rawDataProvider;
    @MockBean
    @SuppressWarnings("unused")
    private RegionConnector regionConnector;
    @MockBean
    @SuppressWarnings("unused")
    private AccountingPointEnvelopeProvider accountingPointEnvelopeProvider;
    @MockBean
    @SuppressWarnings("unused")
    private PermissionMarketDocumentProvider permissionMarketDocumentProvider;
    @MockBean
    @SuppressWarnings("unused")
    private ValidatedHistoricalDataEnvelopeProvider validatedHistoricalDataEnvelopeProvider;

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
                () -> assertTrue(extension.supportsValidatedHistoricalDataMarketDocuments())
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
    @MockBean
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
                () -> assertFalse(extension.supportsValidatedHistoricalDataMarketDocuments())
        );
    }
}
