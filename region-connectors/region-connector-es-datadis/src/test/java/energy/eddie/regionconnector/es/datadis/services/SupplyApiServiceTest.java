package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.mockito.Mockito.*;

class SupplyApiServiceTest {

    @Test
    void fetchSupplyForPermissionRequest_returnsExpectedSupply() {
        // Arrange
        String expectedNif = "expectedNif";
        String meteringPointId = "expectedMeteringPointId";

        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.nif()).thenReturn(expectedNif);
        when(permissionRequest.meteringPointId()).thenReturn(meteringPointId);
        when(permissionRequest.measurementType()).thenReturn(MeasurementType.HOURLY);

        Supply supply = createSupply(meteringPointId, 1);
        SupplyApi supplyApi = mock(SupplyApi.class);
        when(supplyApi.getSupplies(expectedNif, null)).thenReturn(Mono.just(List.of(supply)));

        SupplyApiService supplyApiService = new SupplyApiService(supplyApi);

        // Act & Assert
        StepVerifier.create(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest))
                    .expectNext(supply)
                    .verifyComplete();

        verify(supplyApi).getSupplies(expectedNif, null);
    }

    private Supply createSupply(String meteringPointId, Integer pointType) {
        return new Supply("", meteringPointId, "", "", "", "", LocalDate.now(ZONE_ID_SPAIN), null, pointType, "");
    }
}
