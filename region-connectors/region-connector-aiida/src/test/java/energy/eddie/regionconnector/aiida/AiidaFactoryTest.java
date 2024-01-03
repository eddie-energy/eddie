package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import energy.eddie.regionconnector.shared.exceptions.DataNeedNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaFactoryTest {
    @Mock
    private DataNeedsService mockDataNeedsService;
    @Mock
    private AiidaRegionConnectorService mockService;
    private AiidaFactory aiidaFactory;

    @BeforeEach
    void setUp() {
        PlainAiidaConfiguration config = new PlainAiidaConfiguration("localhost:1234", "testData", "testStatus", "testTerminationPrefix");
        aiidaFactory = new AiidaFactory(config, mockDataNeedsService);
    }

    @Test
    void givenNonExistingDataNeedId_createPermissionRequest_throws() {
        // Given
        var dataNeedId = "NonExisting";
        when(mockDataNeedsService.getDataNeed(dataNeedId)).thenReturn(Optional.empty());

        // Then
        assertThrows(DataNeedNotFoundException.class,
                // When
                () -> aiidaFactory.createPermissionRequest("testConnId", dataNeedId, mockService));
    }

    @Test
    void givenValidInput_createPermissionRequest_returnsAsExpected() throws DataNeedNotFoundException {
        // Given
        var connectionId = "testConnId";
        var dataNeedId = "testDataNeedId";
        var mockDataNeed = mock(DataNeed.class);
        when(mockDataNeed.durationStart()).thenReturn(0);
        when(mockDataNeed.durationOpenEnd()).thenReturn(false);
        when(mockDataNeed.durationEnd()).thenReturn(10);
        when(mockDataNeedsService.getDataNeed(dataNeedId)).thenReturn(Optional.of(mockDataNeed));


        // When
        AiidaPermissionRequest request = aiidaFactory.createPermissionRequest(connectionId, dataNeedId, mockService);

        // Then
        assertDoesNotThrow(() -> UUID.fromString(request.permissionId()));
        assertEquals(connectionId, request.connectionId());
        assertEquals(dataNeedId, request.dataNeedId());
        verify(mockDataNeedsService).getDataNeed(dataNeedId);
    }

    @Test
    void givenValidInput_createPermissionDto_returnsAsExpected() throws DataNeedNotFoundException {
        // Given
        var connectionId = "testConnId";
        var dataNeedId = "testDataNeedId";
        var mockDataNeed = mock(DataNeed.class);
        when(mockDataNeed.durationStart()).thenReturn(3);
        when(mockDataNeed.durationOpenEnd()).thenReturn(true);
        when(mockDataNeed.serviceName()).thenReturn("Test Service");
        when(mockDataNeed.sharedDataIds()).thenReturn(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        when(mockDataNeedsService.getDataNeed(dataNeedId)).thenReturn(Optional.of(mockDataNeed));

        AiidaPermissionRequest request = aiidaFactory.createPermissionRequest(connectionId, dataNeedId, mockService);

        // When
        var dto = aiidaFactory.createPermissionDto(request);

        // Then
        assertDoesNotThrow(() -> UUID.fromString(dto.permissionId()));
        assertEquals(request.permissionId(), dto.permissionId());
        assertEquals(connectionId, dto.connectionId());
        assertEquals(dataNeedId, dto.dataNeedId());
        assertEquals("Test Service", dto.serviceName());
        assertThat(dto.requestedCodes()).hasSameElementsAs(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        assertNotNull(dto.kafkaStreamingConfig());
        assertThat(dto.startTime()).isAfter(Instant.now());
        assertThat(dto.expirationTime()).isAfter(Instant.now());
    }
}