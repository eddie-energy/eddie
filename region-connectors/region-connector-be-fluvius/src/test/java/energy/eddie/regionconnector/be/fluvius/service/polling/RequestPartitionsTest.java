// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.service.polling;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;
import static org.assertj.core.api.Assertions.assertThat;

class RequestPartitionsTest {
    @Test
    void testPartitions_forOneYearWithoutMeterReading_returnsCorrectPartitions() {
        // Given
        var start = LocalDate.of(2023, 12, 20);
        var end = LocalDate.of(2024, 12, 20);
        var pr = new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                Granularity.PT15M,
                start,
                end,
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2B
        );
        var partitions = new RequestPartitions(pr, new MeterReading("pid", "ean", null));
        var startDateTime = start.atStartOfDay(ZoneOffset.UTC);
        var item1 = new RequestPartitions.DateTimePartition(startDateTime, startDateTime.plusDays(184));
        var item2 = new RequestPartitions.DateTimePartition(startDateTime.plusDays(184), endOfDay(end, ZoneOffset.UTC));

        // When
        var parts = partitions.partitions();

        // Then
        assertThat(parts)
                .hasSize(2)
                .isEqualTo(List.of(item1, item2));
    }

    @Test
    void testPartitions_withMeterReading_returnsCorrectPartitions() {
        // Given
        var start = LocalDate.of(2024, 12, 10);
        var end = LocalDate.of(2024, 12, 20);
        var pr = new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                Granularity.PT15M,
                start,
                end,
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2B
        );
        var meterReading = start.plusDays(5).atStartOfDay(ZoneOffset.UTC);
        var partitions = new RequestPartitions(pr, new MeterReading("pid", "ean", meterReading));
        var item = new RequestPartitions.DateTimePartition(meterReading, endOfDay(end, ZoneOffset.UTC));

        // When
        var parts = partitions.partitions();

        // Then
        assertThat(parts)
                .hasSize(1)
                .isEqualTo(List.of(item));
    }

    @Test
    void testPartitions_forFutureData_limitsEndToNow() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var start = now.minusDays(5);
        var end = now.plusDays(5);
        var pr = new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                Granularity.PT15M,
                start,
                end,
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2B
        );
        var partitions = new RequestPartitions(pr, new MeterReading("pid", "ean", null));
        var item = new RequestPartitions.DateTimePartition(start.atStartOfDay(ZoneOffset.UTC),
                                                           now.atStartOfDay(ZoneOffset.UTC));

        // When
        var parts = partitions.partitions();

        // Then
        assertThat(parts)
                .hasSize(1)
                .isEqualTo(List.of(item));
    }

    @Test
    void testPartitions_whereStartAndEndEqual_returnsOnePartition() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                Granularity.PT15M,
                now,
                now,
                ZonedDateTime.now(ZoneOffset.UTC),
                Flow.B2B
        );
        var partitions = new RequestPartitions(pr, new MeterReading("pid", "ean", null));
        var item = new RequestPartitions.DateTimePartition(now.atStartOfDay(ZoneOffset.UTC),
                                                           now.atStartOfDay(ZoneOffset.UTC));

        // When
        var parts = partitions.partitions();

        // Then
        assertThat(parts)
                .hasSize(1)
                .isEqualTo(List.of(item));
    }
}