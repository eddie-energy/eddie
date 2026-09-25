// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import energy.eddie.regionconnector.at.eda.permission.request.events.ImportEvent;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private DataNeedCalculationService calculationService;
    @InjectMocks
    private ImportService importService;
    @Captor
    private ArgumentCaptor<ImportEvent> captor;

    @Test
    void givenCESUJoinRequestDataNeedAndValidPermissionRequest_whenImported_thenReturnPermissionId() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var creationDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z");
        var pr = new PermissionRequestToImport("cid",
                                               "AT0000000000000000000000000000000",
                                               "dnid",
                                               "AT001000",
                                               "Consent-ID",
                                               creationDateTime);
        var start = creationDateTime.toLocalDate().plusDays(1);
        when(calculationService.calculate("dnid", creationDateTime))
                .thenReturn(new CESUJoinRequestDataNeedResult(
                        new Timeframe(start, null),
                        List.of(Granularity.PT15M, Granularity.P1D),
                        new CESUJoinRequestDataNeed()));

        // When
        var res = importService.importPermissionRequest(pr);

        // Then
        verify(outbox).commit(captor.capture());
        var event = captor.getValue();
        assertAll(
                () -> assertEquals(res.permissionIds().getFirst(), event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, event.status()),
                () -> assertEquals("cid", event.connectionId()),
                () -> assertEquals("dnid", event.dataNeedId()),
                () -> assertEquals(new EdaDataSourceInformation("AT001000"), event.dataSourceInformation()),
                () -> assertEquals("Consent-ID", event.cmConsentId()),
                () -> assertEquals(start, event.permissionStart()),
                () -> assertNull(event.permissionEnd()),
                () -> assertEquals(AllowedGranularity.PT15M, event.granularity()),
                () -> assertEquals(creationDateTime, event.created()),
                () -> assertNull(event.meterReadingStart()),
                () -> assertNull(event.meterReadingEnd())
        );
    }


    @Test
    void givenValidatedHistoricalDataDataNeedPermissionRequest_whenImported_thenReturnPermissionId() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var creationDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z");
        var meterReadingEnd = creationDateTime.plusYears(1);
        var pr = new PermissionRequestToImport("cid",
                                               "AT0000000000000000000000000000000",
                                               "dnid",
                                               "AT001000",
                                               "Consent-ID",
                                               creationDateTime,
                                               creationDateTime,
                                               meterReadingEnd);
        var start = creationDateTime.toLocalDate();
        var end = start.plusYears(3);
        when(calculationService.calculate("dnid", creationDateTime))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(
                        List.of(Granularity.PT15M, Granularity.P1D),
                        new Timeframe(start, end),
                        new Timeframe(start, end),
                        new ValidatedHistoricalDataDataNeed()
                ));

        // When
        var res = importService.importPermissionRequest(pr);

        // Then
        verify(outbox).commit(captor.capture());
        var event = captor.getValue();
        assertAll(
                () -> assertEquals(res.permissionIds().getFirst(), event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, event.status()),
                () -> assertEquals("cid", event.connectionId()),
                () -> assertEquals("dnid", event.dataNeedId()),
                () -> assertEquals(new EdaDataSourceInformation("AT001000"), event.dataSourceInformation()),
                () -> assertEquals("Consent-ID", event.cmConsentId()),
                () -> assertEquals(start, event.permissionStart()),
                () -> assertEquals(end, event.permissionEnd()),
                () -> assertEquals(AllowedGranularity.PT15M, event.granularity()),
                () -> assertEquals(creationDateTime, event.created()),
                () -> assertEquals(creationDateTime, event.meterReadingStart()),
                () -> assertEquals(meterReadingEnd, event.meterReadingEnd())
        );
    }

    @Test
    void givenUnsupportedDataNeed_whenImported_thenThrowsUnsupportedDataNeedException() {
        // Given
        var creationDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z");
        var pr = new PermissionRequestToImport("cid",
                                               "AT0000000000000000000000000000000",
                                               "dnid",
                                               "AT001000",
                                               "Consent-ID",
                                               creationDateTime);
        when(calculationService.calculate("dnid", creationDateTime))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(creationDateTime.toLocalDate(),
                                                                            creationDateTime.toLocalDate()),
                                                              new AccountingPointDataNeed()));

        // When, Then
        var res = assertThrows(UnsupportedDataNeedException.class, () -> importService.importPermissionRequest(pr));
        assertEquals(
                "Data need with ID 'dnid' is not supported: Only Data Needs for Validated Historical Data and CESU Join Requests are supported for imports",
                res.getMessage());
    }

    @Test
    void givenDataNeedNotSupportedResult_whenImported_thenThrowsUnsupportedDataNeedException() {
        // Given
        var creationDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z");
        var pr = new PermissionRequestToImport("cid",
                                               "AT0000000000000000000000000000000",
                                               "dnid",
                                               "AT001000",
                                               "Consent-ID",
                                               creationDateTime);
        when(calculationService.calculate("dnid", creationDateTime))
                .thenReturn(new DataNeedNotSupportedResult("not supported"));

        // When, Then
        var res = assertThrows(UnsupportedDataNeedException.class, () -> importService.importPermissionRequest(pr));
        assertEquals("Region connector 'at-eda' does not support data need with ID 'dnid': not supported",
                     res.getMessage());
    }

    @Test
    void givenDataNeedNotFound_whenImported_thenThrowsDataNeedNotFoundException() {
        // Given
        var creationDateTime = ZonedDateTime.parse("2024-01-01T00:00:00Z");
        var pr = new PermissionRequestToImport("cid",
                                               "AT0000000000000000000000000000000",
                                               "dnid",
                                               "AT001000",
                                               "Consent-ID",
                                               creationDateTime);
        when(calculationService.calculate("dnid", creationDateTime))
                .thenReturn(new DataNeedNotFoundResult());

        // When, Then
        assertThrows(DataNeedNotFoundException.class, () -> importService.importPermissionRequest(pr));
    }
}