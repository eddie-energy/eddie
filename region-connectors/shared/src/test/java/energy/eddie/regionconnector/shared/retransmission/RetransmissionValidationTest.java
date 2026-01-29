// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.retransmission;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetransmissionValidationTest {
    @Mock
    private RegionConnectorMetadata metadata;
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private RetransmissionValidation validation;

    @BeforeEach
    void setUp() {
        when(metadata.timeZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void testValidate_whenPermissionRequestNotFound_returnsPermissionRequestNotFound() {
        // Given
        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var request = new RetransmissionRequest("rc-id", "pid", today, today);
        Optional<SimplePermissionRequest> pr = Optional.empty();

        // When
        var res = validation.validate(pr, request);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(PermissionRequestNotFound.class))
                .satisfies(result -> {
                               assertThat(result.permissionId()).isEqualTo("pid");
                               assertThat(result.timestamp()).isBetween(before, ZonedDateTime.now(ZoneOffset.UTC));
                           }
                );
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"ACCEPTED", "FULFILLED"}, mode = EnumSource.Mode.EXCLUDE)
    void testValidate_forNonAcceptedOrFulfilledRequest_returnsNoActivePermission(PermissionProcessStatus status) {
        // Given
        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = permissionRequest(before.toLocalDate(), today, status);

        var request = new RetransmissionRequest("rc-id", "pid", today, today);

        // When
        var res = validation.validate(Optional.of(permissionRequest), request);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(NoActivePermission.class))
                .satisfies(result -> {
                               assertThat(result.permissionId()).isEqualTo("pid");
                               assertThat(result.timestamp()).isBetween(before, ZonedDateTime.now(ZoneOffset.UTC));
                           }
                );
    }


    @Test
    void testValidate_forAccountingPointDataNeed_returnsNotSupported() {
        // Given
        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = permissionRequest(today, today, PermissionProcessStatus.ACCEPTED);
        when(dataNeedsService.getById("dnid")).thenReturn(new AccountingPointDataNeed());

        var request = new RetransmissionRequest("rc-id", "pid", today, today);

        // When
        var res = validation.validate(Optional.of(permissionRequest), request);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(NotSupported.class))
                .satisfies(result -> {
                               assertThat(result.permissionId()).isEqualTo("pid");
                               assertThat(result.timestamp()).isBetween(before, ZonedDateTime.now(ZoneOffset.UTC));
                               assertThat(result.reason()).isEqualTo(
                                       "Retransmission of data for AccountingPointDataNeed not supported");
                           }
                );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest
    @MethodSource("retransmissionOutsidePermissionTimeFrame")
    void testValidate_whenRetransmissionRequestOutsideOfPermissionTimeFrame_returnsNoPermissionForTimeFrame(
            LocalDate permissionStart,
            LocalDate permissionEnd,
            LocalDate retransmissionFrom,
            LocalDate retransmissionTo,
            String reason
    ) {
        // Then
        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var permissionRequest = permissionRequest(permissionStart, permissionEnd, PermissionProcessStatus.ACCEPTED);
        when(dataNeedsService.getById("dnid")).thenReturn(validatedHistoricalData());

        var request = new RetransmissionRequest("rc-id", "pid", retransmissionFrom, retransmissionTo);
        // When
        var res = validation.validate(Optional.of(permissionRequest), request);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(NoPermissionForTimeFrame.class))
                .satisfies(result -> {
                               assertThat(result.permissionId()).isEqualTo("pid");
                               assertThat(result.timestamp()).isBetween(before, ZonedDateTime.now(ZoneOffset.UTC));
                           }
                );
    }

    @ParameterizedTest
    @MethodSource("retransmissionToInvalidDate")
    void testValidate_retransmissionToIsToday_returnsNotSupported(LocalDate retransmissionTo) {
        // Given
        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var today = LocalDate.now(ZoneOffset.UTC);
        var permissionRequest = permissionRequest(today.minusWeeks(1),
                                                  today.plusMonths(1),
                                                  PermissionProcessStatus.ACCEPTED);
        when(dataNeedsService.getById("dnid")).thenReturn(validatedHistoricalData());

        var request = new RetransmissionRequest("rc-id", "pid", today.minusDays(1), retransmissionTo);

        // When
        var res = validation.validate(Optional.of(permissionRequest), request);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(NotSupported.class))
                .satisfies(result -> {
                               assertThat(result.permissionId()).isEqualTo("pid");
                               assertThat(result.timestamp()).isBetween(before, ZonedDateTime.now(ZoneOffset.UTC));
                               assertThat(result.reason()).isEqualTo("Retransmission to date needs to be before today");
                           }
                );
    }

    @ParameterizedTest
    @EnumSource(value = PermissionProcessStatus.class, names = {"FULFILLED", "ACCEPTED"})
    void testValidate_returnsSuccess(PermissionProcessStatus status) {
        var start = LocalDate.of(2025, 1, 1);
        var end = LocalDate.of(2025, 1, 31);
        var retransmitFrom = start.plusDays(2);
        var retransmitTo = start.plusDays(4);

        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var permissionRequest = permissionRequest(start, end, status);
        when(dataNeedsService.getById("dnid")).thenReturn(validatedHistoricalData());

        var request = new RetransmissionRequest("rc-id", "pid", retransmitFrom, retransmitTo);

        // When
        var res = validation.validate(Optional.of(permissionRequest), request);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(Success.class))
                .satisfies(result -> {
                               assertThat(result.permissionId()).isEqualTo("pid");
                               assertThat(result.timestamp()).isBetween(before, ZonedDateTime.now(ZoneOffset.UTC));
                           }
                );
    }

    private static ValidatedHistoricalDataDataNeed validatedHistoricalData() {
        return new ValidatedHistoricalDataDataNeed(new RelativeDuration(null,
                                                                        null,
                                                                        null),
                                                   EnergyType.ELECTRICITY,
                                                   Granularity.PT5M,
                                                   Granularity.P1Y);
    }

    private static SimplePermissionRequest permissionRequest(
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus status
    ) {
        return new SimplePermissionRequest("pid",
                                           "cid",
                                           "dnid",
                                           start,
                                           end,
                                           ZonedDateTime.now(ZoneOffset.UTC),
                                           status);
    }

    private static Stream<Arguments> retransmissionOutsidePermissionTimeFrame() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var startDate = today.minusDays(5);
        var endDate = today.plusDays(5);
        return Stream.of(
                Arguments.of(startDate,
                             endDate,
                             startDate.minusDays(2),
                             startDate.minusDays(1),
                             "completely before start"),
                Arguments.of(startDate, endDate, startDate.minusDays(1), endDate.minusDays(1), "from before start"),
                Arguments.of(startDate, endDate, startDate.plusDays(1), endDate.plusDays(1), "to after end"),
                Arguments.of(startDate, endDate, endDate.plusDays(1), endDate.plusDays(2), "completely after end"),
                Arguments.of(startDate,
                             endDate,
                             startDate.minusDays(1),
                             endDate.plusDays(1),
                             "from before start & to after end")
        );
    }

    private static Stream<Arguments> retransmissionToInvalidDate() {
        var today = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(today),
                Arguments.of(today.plusDays(1)),
                Arguments.of(today.plusWeeks(1))
        );
    }
}