package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.at.eda.AtEdaBeanConfig;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

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
    private ValidatedHistoricalDataDataNeed vhdDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    @Mock
    private GenericAiidaDataNeed unsupportedDataNeed;
    @Mock
    private AbsoluteDuration absoluteDuration;
    @InjectMocks
    private PermissionRequestCreationAndValidationService creationService;
    @SuppressWarnings("unused")
    @Spy
    private DataNeedCalculationService<DataNeed> dataNeedCalculationService = new AtEdaBeanConfig().dataNeedCalculationService();
    @SuppressWarnings("unused")
    @Spy
    private AtConfiguration configuration = new AtEdaBeanConfig().atConfiguration("epId");

    @Test
    void createValidPermissionRequest_forHVDDataNeed() throws DataNeedNotFoundException, UnsupportedDataNeedException, EdaValidationException {
        // Given
        when(mockService.findById(any())).thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.minGranularity()).thenReturn(Granularity.PT15M);
        when(vhdDataNeed.maxGranularity()).thenReturn(Granularity.PT15M);
        LocalDate start = LocalDate.now(AT_ZONE_ID).minusDays(10);
        when(vhdDataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(start);
        when(absoluteDuration.end()).thenReturn(start.plusDays(5));


        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertNotNull(res);
    }

    @Test
    void createValidPermissionRequest_forAccountingPointDataNeed() throws DataNeedNotFoundException, UnsupportedDataNeedException, EdaValidationException {
        // Given
        when(mockService.findById(any())).thenReturn(Optional.of(accountingPointDataNeed));
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertNotNull(res);
    }

    @Test
    void createInvalidPermissionRequest() {
        // Given
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT1234500699900000000000206868100",
                                                                           "dnid", "AT000000");


        // When
        // Then
        assertThrows(EdaValidationException.class, () -> creationService.createAndValidatePermissionRequest(pr));
        verify(mockOutbox, times(2)).commit(any());
    }

    @Test
    void unsupportedDataNeedThrows() {
        // Given
        when(mockService.findById(any())).thenReturn(Optional.of(unsupportedDataNeed));

        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");
        // When & Then
        assertThrows(UnsupportedDataNeedException.class, () -> creationService.createAndValidatePermissionRequest(pr));
    }

    @Test
    void givenInvalidGranularity_createAndValidatePermissionRequest_throwsException() {
        // Given
        when(mockService.findById(any())).thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.minGranularity()).thenReturn(Granularity.PT5M);
        when(vhdDataNeed.maxGranularity()).thenReturn(Granularity.PT5M);
        when(vhdDataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(LocalDate.now(AT_ZONE_ID).minusDays(5));
        when(absoluteDuration.end()).thenReturn(LocalDate.now(AT_ZONE_ID).minusDays(1));
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class, () -> creationService.createAndValidatePermissionRequest(pr));
    }

    @Test
    void givenInvalidStart_createAndValidatePermissionRequest_throwsException() {
        // Given
        when(mockService.findById(any())).thenReturn(Optional.of(vhdDataNeed));
        when(vhdDataNeed.duration()).thenReturn(absoluteDuration);
        when(vhdDataNeed.minGranularity()).thenReturn(Granularity.PT15M);
        when(vhdDataNeed.maxGranularity()).thenReturn(Granularity.P1D);
        when(absoluteDuration.start()).thenReturn(LocalDate.now(AT_ZONE_ID).minusYears(5));
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class, () -> creationService.createAndValidatePermissionRequest(pr));
    }
}
