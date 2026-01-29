// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider;

import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.projections.MeterReadingTimeframe;
import energy.eddie.regionconnector.at.eda.persistence.MeterReadingTimeframeRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicateDataFilterTest {
    @Mock
    private MeterReadingTimeframeRepository repository;
    @InjectMocks
    private DuplicateDataFilter filter;

    public static Stream<Arguments> testApply_removesPermissionRequestThatAlreadyReceivedTheData() {
        var commonStart = LocalDate.of(2024, 1, 1);
        var commonEnd = LocalDate.of(2024, 1, 10);
        return Stream.of(
                Arguments.of(commonStart, commonEnd, commonStart, commonEnd),
                Arguments.of(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 9), commonStart, commonEnd)
        );
    }

    public static Stream<Arguments> testApply_doesNotRemoveIfDataDoesNotOverlapCompletely() {
        return Stream.of(
                Arguments.of(LocalDate.of(2024, 1, 10),
                             LocalDate.of(2024, 1, 20),
                             LocalDate.of(2024, 1, 15),
                             LocalDate.of(2024, 1, 25)),
                Arguments.of(LocalDate.of(2024, 1, 10),
                             LocalDate.of(2024, 1, 20),
                             LocalDate.of(2024, 1, 5),
                             LocalDate.of(2024, 1, 15))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testApply_removesPermissionRequestThatAlreadyReceivedTheData(
            LocalDate dataStart,
            LocalDate dataEnd,
            LocalDate readingStart,
            LocalDate readingEnd
    ) {
        // Given
        var id = new IdentifiableConsumptionRecord(new SimpleEdaConsumptionRecord(),
                                                   List.of(createPermissionRequest()),
                                                   dataStart,
                                                   dataEnd);
        when(repository.findAllByPermissionId("pid"))
                .thenReturn(List.of(new MeterReadingTimeframe(1L, "pid", readingStart, readingEnd)));

        // When
        var res = filter.apply(id);

        // Then
        assertThat(res.permissionRequests())
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource
    void testApply_doesNotRemoveIfDataDoesNotOverlapCompletely(
            LocalDate dataStart,
            LocalDate dataEnd,
            LocalDate readingStart,
            LocalDate readingEnd
    ) {
        // Given
        var id = new IdentifiableConsumptionRecord(new SimpleEdaConsumptionRecord(),
                                                   List.of(createPermissionRequest()),
                                                   dataStart,
                                                   dataEnd);
        when(repository.findAllByPermissionId("pid"))
                .thenReturn(List.of(new MeterReadingTimeframe(1L, "pid", readingStart, readingEnd)));

        // When
        var res = filter.apply(id);

        // Then
        assertThat(res).isEqualTo(id);
    }


    private static SimplePermissionRequest createPermissionRequest() {
        return new SimplePermissionRequest("pid", "cid", "dnid");
    }
}