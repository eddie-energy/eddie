package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestCreationAndValidationServiceTest {
    @Mock
    private Outbox mockOutbox;
    @Mock
    private DataNeedsService mockService;
    @Mock
    private ValidatedHistoricalDataDataNeed mockDataNeed;
    @Mock
    private DataNeedWrapper mockWrapper;
    @Mock
    private AtConfiguration mockConfig;
    @InjectMocks
    private PermissionRequestCreationAndValidationService creationService;

    @Test
    void createValidPermissionRequest() throws ValidationException, DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        when(mockService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(mockWrapper);
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT15M);
        when(mockWrapper.timeframedDataNeed()).thenReturn(mockDataNeed);
        LocalDate start = LocalDate.now(AT_ZONE_ID).minusDays(10);
        when(mockWrapper.calculatedStart()).thenReturn(start);
        when(mockWrapper.calculatedEnd()).thenReturn(start.plusDays(5));
        when(mockConfig.eligiblePartyId()).thenReturn("AT999999");


        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertNotNull(res);
    }

    @Test
    void createInvalidPermissionRequest() throws DataNeedNotFoundException {
        // Given
        when(mockService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(mockWrapper);
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT15M);
        when(mockWrapper.timeframedDataNeed()).thenReturn(mockDataNeed);
        LocalDate start = LocalDate.now(AT_ZONE_ID).minusDays(10);
        when(mockWrapper.calculatedStart()).thenReturn(start);
        when(mockWrapper.calculatedEnd()).thenReturn(start.plusDays(5));
        when(mockConfig.eligiblePartyId()).thenReturn("AT999999");


        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT1234500699900000000000206868100",
                                                                           "dnid", "AT000000");


        // When
        // Then
        assertThrows(ValidationException.class, () -> creationService.createAndValidatePermissionRequest(pr));
        verify(mockOutbox, times(2)).commit(any());
    }

    @Test
    void givenInvalidGranularity_createAndValidatePermissionRequest_throwsException() throws DataNeedNotFoundException {
        // Given
        when(mockWrapper.timeframedDataNeed()).thenReturn(mockDataNeed);
        when(mockService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(mockWrapper);
        when(mockDataNeed.minGranularity()).thenReturn(Granularity.PT5M);
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class, () -> {
            creationService.createAndValidatePermissionRequest(pr);
        });
    }
}
