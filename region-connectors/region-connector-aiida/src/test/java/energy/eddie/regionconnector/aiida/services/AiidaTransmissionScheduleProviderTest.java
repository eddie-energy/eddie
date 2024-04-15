package energy.eddie.regionconnector.aiida.services;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiidaTransmissionScheduleProviderTest {

    @Test
    void testFindTransmissionSchedule_returnsISODuration() {
        // Given
        var dataNeed = mock(AiidaDataNeed.class);
        when(dataNeed.transmissionInterval()).thenReturn(10);
        var dataNeedsService = mock(DataNeedsService.class);
        when(dataNeedsService.findById("dataNeedId"))
                .thenReturn(Optional.of(dataNeed));
        var permissionRequest = mock(AiidaPermissionRequest.class);
        when(permissionRequest.dataNeedId()).thenReturn("dataNeedId");
        var scheduleProvider = new AiidaTransmissionScheduleProvider(dataNeedsService);

        // When
        var res = scheduleProvider.findTransmissionSchedule(permissionRequest);

        // Then
        assertEquals("PT10S", res);
    }
}
