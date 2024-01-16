package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataNeedsService;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
        var fixedClock = Clock.fixed(Instant.parse("2023-10-15T15:00:00Z"), ZoneId.of("UTC"));
        aiidaFactory = new AiidaFactory(config, mockDataNeedsService, fixedClock, Set.of());
    }

    @Test
    void givenNonExistingDataNeedId_createPermissionRequest_throws() {
        // Given
        var dataNeedId = "NonExisting";
        when(mockDataNeedsService.getDataNeed(dataNeedId)).thenReturn(Optional.empty());

        // Then
        assertThrows(DataNeedNotFoundException.class,
                // When
                () -> aiidaFactory.createPermissionRequest("testConnId", dataNeedId));
    }

    @Test
    void givenValidInput_createPermissionRequest_returnsAsExpected() throws DataNeedNotFoundException {
        // Given
        var connectionId = "testConnId";
        var dataNeedId = "testDataNeedId";
        var mockDataNeed = mock(DataNeed.class);
        when(mockDataNeed.durationStart()).thenReturn(-5);
        when(mockDataNeed.durationOpenEnd()).thenReturn(false);
        when(mockDataNeed.durationEnd()).thenReturn(10);
        when(mockDataNeedsService.getDataNeed(dataNeedId)).thenReturn(Optional.of(mockDataNeed));

        var expectedStart = Instant.parse("2023-10-10T00:00:00Z");
        var expectedExpiration = Instant.parse("2023-10-25T23:59:59Z");


        // When
        AiidaPermissionRequestInterface request = aiidaFactory.createPermissionRequest(connectionId, dataNeedId);

        // Then
        assertDoesNotThrow(() -> UUID.fromString(request.permissionId()));
        assertEquals(connectionId, request.connectionId());
        assertEquals(dataNeedId, request.dataNeedId());
        assertEquals(expectedStart, request.startTime());
        assertEquals(expectedExpiration, request.expirationTime());
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

        var expectedStart = Instant.parse("2023-10-18T00:00:00Z");
        // when open end, 1000 years get added to now()
        var expectedExpiration = Instant.parse("3023-10-15T23:59:59Z");

        AiidaPermissionRequestInterface request = aiidaFactory.createPermissionRequest(connectionId, dataNeedId);


        // When
        var dto = aiidaFactory.createPermissionDto(request);


        // Then
        assertEquals(expectedStart, request.startTime());
        assertEquals(expectedExpiration, request.expirationTime());

        assertDoesNotThrow(() -> UUID.fromString(dto.permissionId()));
        assertEquals(request.permissionId(), dto.permissionId());
        assertEquals(connectionId, dto.connectionId());
        assertEquals(dataNeedId, dto.dataNeedId());
        assertEquals("Test Service", dto.serviceName());
        assertThat(dto.requestedCodes()).hasSameElementsAs(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        assertNotNull(dto.kafkaStreamingConfig());
        assertEquals(expectedStart, dto.startTime());
        assertEquals(expectedExpiration, dto.expirationTime());
    }
}