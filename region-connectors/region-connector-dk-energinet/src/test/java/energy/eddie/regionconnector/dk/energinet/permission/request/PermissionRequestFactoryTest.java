package energy.eddie.regionconnector.dk.energinet.permission.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
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
    private AbsoluteDuration absoluteDuration;
    @Mock
    private ValidatedHistoricalDataDataNeed mockDataNeed;
    @Mock
    private GenericAiidaDataNeed mockAiidaNeed;
    @Mock
    private ObjectMapper mockMapper;

    @Test
    void testCreatePermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var requestForCreation = new PermissionRequestForCreation("foo", "token", "bar", "foo");
        when(mockDataNeedsService.findById(any())).thenReturn(Optional.of(mockDataNeed));
        when(mockDataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(LocalDate.now(DK_ZONE_ID));
        when(absoluteDuration.end()).thenReturn(LocalDate.now(DK_ZONE_ID).plusDays(5));
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT15M);

        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService,
                                                                    mockMapper);
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
                                                                       new StateBuilderFactory(),
                                                                       mockMapper);
        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService,
                                                                    mockMapper);

        // When
        PermissionRequest wrapped = permissionRequestFactory.create(permissionRequest);

        // Then
        assertNotNull(wrapped);
    }

    @Test
    void givenUnsupportedDataNeedType_throws() {
        // Given
        when(mockDataNeedsService.findById(any())).thenReturn(Optional.of(mockAiidaNeed));
        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService,
                                                                    mockMapper);
        var creation = new PermissionRequestForCreation("foo", "bar", "blo", "lah");

        // When, Then
        var thrown = assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestFactory.create(creation));

        assertThat(thrown.getMessage()).contains(
                "This region connector only supports validated historical data data needs.");
    }

    @Test
    void givenUnsupportedGranularity_throws() {
        // Given
        when(mockDataNeedsService.findById(any())).thenReturn(Optional.of(mockDataNeed));
        when(mockDataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(LocalDate.now(DK_ZONE_ID));
        when(absoluteDuration.end()).thenReturn(LocalDate.now(DK_ZONE_ID).plusDays(5));
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT5M);
        var permissionRequestFactory = new PermissionRequestFactory(customerApi,
                                                                    Set.of(),
                                                                    new StateBuilderFactory(),
                                                                    mockDataNeedsService,
                                                                    mockMapper);
        var creation = new PermissionRequestForCreation("foo", "bar", "blo", "lah");

        // When, Then
        var thrown = assertThrows(UnsupportedDataNeedException.class, () -> permissionRequestFactory.create(creation));

        assertThat(thrown.getMessage()).contains("Unsupported granularity: 'PT5M'");
    }
}
