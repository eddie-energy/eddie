// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEventFactory;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestCreationAndValidationServiceTest {
    @Spy
    private final AtConfiguration configuration = new AtConfiguration("epId");
    @SuppressWarnings("unused")
    @Spy
    private final ValidatedEventFactory validatedEventFactory = new ValidatedEventFactory(configuration);
    @SuppressWarnings("unused")
    @Mock
    private Outbox mockOutbox;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @InjectMocks
    private PermissionRequestCreationAndValidationService creationService;

    @Test
    void createValidPermissionRequest_forHVDDataNeed() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var start = LocalDate.now(AT_ZONE_ID).minusDays(10);
        var timeframe = new Timeframe(start, start.plusDays(5));
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                      timeframe,
                                                                      timeframe));


        var pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                  List.of("dnid"), "AT000000");

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertNotNull(res);
    }


    @Test
    void createValidPermissionRequestAndInvalidPermissionRequest_forHVDDataNeed() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var start = LocalDate.now(AT_ZONE_ID).minusDays(10);
        var timeframe = new Timeframe(start, start.plusDays(5));
        when(calculationService.calculate("dnid1"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                      timeframe,
                                                                      timeframe));
        when(calculationService.calculate("dnid2"))
                .thenReturn(new DataNeedNotSupportedResult("msg"));


        var pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                  List.of("dnid1", "dnid2"), "AT000000");

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertThat(res.permissionIds()).hasSize(1);
    }

    @Test
    void createValidPermissionRequest_forAccountingPointDataNeed() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now, now);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(timeframe));
        var pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                  List.of("dnid"), "AT000000");

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertNotNull(res);
    }

    @Test
    void unsupportedDataNeedThrows() {
        // Given
        when(calculationService.calculate("dnid")).thenReturn(new DataNeedNotSupportedResult("bla"));

        var pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                  List.of("dnid"), "AT000000");
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> creationService.createAndValidatePermissionRequest(pr));
    }

    @Test
    void givenInvalidGranularity_createAndValidatePermissionRequest_throwsException() {
        // Given
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotSupportedResult("Granularities not supported"));
        var pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                  List.of("dnid"), "AT000000");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class, () -> creationService.createAndValidatePermissionRequest(pr));
    }

    @Test
    void givenUnknownDataNeedId_throwsUnknownDataNeedException() {
        // Given
        var pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                  List.of("dnid"), "AT000000");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotFoundResult());

        // When, Then
        assertThrows(DataNeedNotFoundException.class, () -> creationService.createAndValidatePermissionRequest(pr));
    }
}
