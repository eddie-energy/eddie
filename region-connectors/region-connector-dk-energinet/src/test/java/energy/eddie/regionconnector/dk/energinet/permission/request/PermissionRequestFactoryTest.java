package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Set;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestFactoryTest {
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private DataNeedsService mockDataNeedsService;
    @Mock
    private DataNeedWrapper mockWrapper;
    @Mock
    private ValidatedHistoricalDataDataNeed mockDataNeed;
    @Mock
    private GenericAiidaDataNeed mockAiidaNeed;

    @Test
    void testCreatePermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var requestForCreation = new PermissionRequestForCreation("foo", "token", "bar", "foo");
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(
                mockWrapper);
        when(mockWrapper.calculatedStart()).thenReturn(LocalDate.now(DK_ZONE_ID));
        when(mockWrapper.calculatedEnd()).thenReturn(LocalDate.now(DK_ZONE_ID).plusDays(5));
        when(mockWrapper.timeframedDataNeed()).thenReturn(mockDataNeed);
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT15M);

        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService);
        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(requestForCreation);

        // Then
        assertNotNull(permissionRequest);
    }

    @Test
    void testRecreatePermissionRequest_doesNotRunExtension() {
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var requestForCreation = new PermissionRequestForCreation("foo", "token", "bar", "foo");
        EnerginetCustomerApi customerApi = mock(EnerginetCustomerApi.class);
        var permissionRequest = new EnerginetCustomerPermissionRequest("pid",
                                                                       requestForCreation,
                                                                       customerApi,
                                                                       start,
                                                                       end,
                                                                       Granularity.PT1H,
                                                                       new StateBuilderFactory());
        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService);

        // When
        PermissionRequest wrapped = permissionRequestFactory.create(permissionRequest);

        // Then
        assertNotNull(wrapped);
    }

    @Test
    void givenUnsupportedDataNeedType_throws() throws DataNeedNotFoundException {
        // Given
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(
                mockWrapper);
        when(mockWrapper.timeframedDataNeed()).thenReturn(mockAiidaNeed);
        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService);
        var creation = new PermissionRequestForCreation("foo", "bar", "blo", "lah");

        // When, Then
        var thrown = assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestFactory.create(creation));

        assertThat(thrown.getMessage()).contains(
                "This region connector only supports validated historical data data needs.");
    }

    @Test
    void givenUnsupportedGranularity_throws() throws DataNeedNotFoundException {
        // Given
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(
                mockWrapper);
        when(mockWrapper.timeframedDataNeed()).thenReturn(mockDataNeed);
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT5M);
        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService);
        var creation = new PermissionRequestForCreation("foo", "bar", "blo", "lah");

        // When, Then
        var thrown = assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestFactory.create(creation));

        assertThat(thrown.getMessage()).contains("Unsupported granularity: 'PT5M'");
    }
}
