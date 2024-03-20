package energy.eddie.regionconnector.aiida;

import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiidaFactoryTest {
    private final String connectionId = "testConnId";
    private final String dataNeedId = "testDataNeedId";
    @Mock
    private DataNeedsService mockService;
    @Mock
    private GenericAiidaDataNeed mockDataNeed;
    @Mock
    private ValidatedHistoricalDataDataNeed unsupportedDataNeed;
    @Mock
    private RelativeDuration mockRelativeDuration;
    private AiidaFactory aiidaFactory;

    @BeforeEach
    void setUp() {
        PlainAiidaConfiguration config = new PlainAiidaConfiguration(
                "localhost:1234",
                "testData",
                "testStatus",
                "testTerminationPrefix",
                "customerId"
        );
        var fixedClock = Clock.fixed(Instant.parse("2023-10-15T15:00:00Z"),
                                     AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID);
        aiidaFactory = new AiidaFactory(config, mockService, fixedClock, Set.of());
    }

    @Test
    void givenNonExistingDataNeedId_createPermissionRequest_throwsException() throws DataNeedNotFoundException {
        // Given
        var dataNeedId = "NonExisting";
        when(mockService.findDataNeedAndCalculateStartAndEnd(anyString(), any(), any(), any())).thenThrow(
                DataNeedNotFoundException.class);

        // Then
        assertThrows(DataNeedNotFoundException.class,
                     // When
                     () -> aiidaFactory.createPermissionRequest("testConnId", dataNeedId));
    }

    @Test
    void givenUnsupportedDataNeed_createPermissionRequest_throwsException() throws DataNeedNotFoundException {
        // Given
        DataNeedWrapper wrapper = new DataNeedWrapper(unsupportedDataNeed,
                                                      LocalDate.now(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID),
                                                      LocalDate.now(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID));
        when(mockService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> aiidaFactory.createPermissionRequest(connectionId, dataNeedId));
    }

    @Test
    void givenOpenStartEndDataNeed_createPermissionRequest_usesFixedValues() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        when(mockDataNeed.duration()).thenReturn(mockRelativeDuration);
        when(mockRelativeDuration.start()).thenReturn(Optional.empty());
        when(mockRelativeDuration.end()).thenReturn(Optional.empty());
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed,
                                                      LocalDate.now(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID),
                                                      LocalDate.now(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID));
        when(mockService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        // When
        var request = aiidaFactory.createPermissionRequest(connectionId, dataNeedId);

        // Then
        assertEquals(LocalDate.parse("2000-01-01"), request.start());
        assertEquals(LocalDate.parse("9999-12-31"), request.end());
    }

    @Test
    void givenRelativeDuration_createPermissionRequest_usesCalculatedStartAndEnd() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        when(mockDataNeed.duration()).thenReturn(mockRelativeDuration);
        when(mockRelativeDuration.start()).thenReturn(Optional.of(Period.parse("P-5D")));
        when(mockRelativeDuration.end()).thenReturn(Optional.of(Period.parse("P10D")));
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 4));
        when(mockService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        var expectedStart = LocalDate.parse("2024-04-01");
        var expectedExpiration = LocalDate.parse("2024-04-04");


        // When
        AiidaPermissionRequestInterface request = aiidaFactory.createPermissionRequest(connectionId, dataNeedId);

        // Then
        assertDoesNotThrow(() -> UUID.fromString(request.permissionId()));
        assertEquals(connectionId, request.connectionId());
        assertEquals(dataNeedId, request.dataNeedId());
        assertEquals(expectedStart, request.start());
        assertEquals(expectedExpiration, request.end());
    }

    @Test
    void givenValidInput_createPermissionDto_returnsAsExpected() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        when(mockDataNeed.name()).thenReturn("Test Service");
        when(mockDataNeed.dataTags()).thenReturn(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        when(mockService.findById(anyString())).thenReturn(Optional.of(mockDataNeed));
        var start = LocalDate.parse("2023-01-01");
        var end = LocalDate.parse("2023-01-25");


        String permissionId = "8390505d-2bb1-4321-bad2-396897cad525";
        AiidaPermissionRequest request = new AiidaPermissionRequest(permissionId,
                                                                    connectionId,
                                                                    dataNeedId,
                                                                    "foo",
                                                                    start,
                                                                    end);
        // When
        var dto = aiidaFactory.createPermissionDto(request);


        // Then
        assertAll(
                () -> assertDoesNotThrow(() -> UUID.fromString(dto.permissionId())),
                () -> assertEquals(request.start(), dto.startDate()),
                () -> assertEquals(request.end(), dto.expirationDate()),
                () -> assertEquals(request.permissionId(), dto.permissionId()),
                () -> assertEquals(connectionId, dto.connectionId()),
                () -> assertEquals(dataNeedId, dto.dataNeedId()),
                () -> assertEquals("Test Service", dto.serviceName()),
                () -> assertThat(dto.requestedCodes()).hasSameElementsAs(Set.of("1-0:1.8.0", "1-0:1.7.0")),
                () -> assertNotNull(dto.kafkaStreamingConfig())
        );
    }
}
