package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.TestPollingEvent;
import energy.eddie.regionconnector.shared.permission.requests.DummyDataSourceInformation;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MeterReadingPermissionRequestUpdateAndFulfillmentServiceTest {

    private static final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    @Mock
    private FulfillmentService FulfillmentService;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<TestPollingEvent> eventCaptor;

    public static Stream<Arguments> latestMeterReadingEndDates() {
        return Stream.of(
                Arguments.of((LocalDate) null),
                Arguments.of(today.minusDays(1)),
                Arguments.of(today)
        );
    }

    @ParameterizedTest
    @MethodSource("latestMeterReadingEndDates")
    void tryUpdateAndFulfillPermissionRequest_withMeterReadingEndSameAsPermission_LatestAsExpectedButDoesNotFulfill(@Nullable LocalDate latest) {
        // Given
        var end = today.plusDays(1);
        var permissionRequest = acceptedPermissionRequest(end, latest);

        var service = new MeterReadingPermissionUpdateAndFulfillmentService(
                FulfillmentService,
                this::commitEvent);

        // When
        service.tryUpdateAndFulfillPermissionRequest(permissionRequest, () -> end);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertEquals(end, res.lastPolled());
    }

    private static MeterReadingPermissionRequest acceptedPermissionRequest(LocalDate end, LocalDate latest) {
        return new SimpleMeterReadingPermissionRequest(
                MeterReadingPermissionRequestUpdateAndFulfillmentServiceTest.today, end, latest);
    }

    @SuppressWarnings("DirectInvocationOnMock")
    private void commitEvent(MeterReadingPermissionRequest pr, LocalDate endDate) {
        outbox.commit(new TestPollingEvent(pr.permissionId(),
                                           PermissionProcessStatus.ACCEPTED,
                                           endDate));
    }

    @Test
    void tryUpdateAndFulfillPermissionRequest_withMeterReadingEndBeforeLatest_DoesNotUpdateLatestAndDoesNotFulfill() {
        // Given
        var latest = today.minusDays(2);
        var permissionRequest = acceptedPermissionRequest(latest, latest);

        var service = new MeterReadingPermissionUpdateAndFulfillmentService(FulfillmentService,
                                                                            (pr, endDate) -> outbox.commit(new TestPollingEvent(
                                                                                    pr.permissionId(),
                                                                                    PermissionProcessStatus.ACCEPTED,
                                                                                    endDate)));

        // When
        service.tryUpdateAndFulfillPermissionRequest(permissionRequest, () -> latest.minusDays(1));

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void tryUpdateAndFulfillPermissionRequest_withMeterReadingEndAfterPermission_UpdatesLatestAndFulfills() {
        // Given
        var end = today.plusDays(1);
        var permissionRequest = acceptedPermissionRequest(today, null);

        var service = new MeterReadingPermissionUpdateAndFulfillmentService(FulfillmentService,
                                                                            (pr, endDate) -> outbox.commit(new TestPollingEvent(
                                                                                    pr.permissionId(),
                                                                                    PermissionProcessStatus.ACCEPTED,
                                                                                    endDate)));

        // When
        service.tryUpdateAndFulfillPermissionRequest(permissionRequest, () -> end);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertEquals(end, res.lastPolled());
    }

    private static class SimpleMeterReadingPermissionRequest implements MeterReadingPermissionRequest {
        private final LocalDate start;
        private final LocalDate end;
        private final PermissionProcessStatus status;
        @Nullable
        private final LocalDate latestMeterReadingEndDate;


        public SimpleMeterReadingPermissionRequest(
                LocalDate start,
                LocalDate end,
                @Nullable LocalDate latest
        ) {
            this.start = start;
            this.end = end;
            this.latestMeterReadingEndDate = latest;
            this.status = PermissionProcessStatus.ACCEPTED;
        }

        @Override
        public Optional<LocalDate> latestMeterReadingEndDate() {
            return Optional.ofNullable(latestMeterReadingEndDate);
        }

        @Override
        public String permissionId() {
            return "permissionId";
        }

        @Override
        public String connectionId() {
            return "connectionId";
        }

        @Override
        public String dataNeedId() {
            return "dataNeedId";
        }

        @Override
        public PermissionProcessStatus status() {
            return status;
        }

        @Override
        public DataSourceInformation dataSourceInformation() {
            return new DummyDataSourceInformation();
        }

        @Override
        public ZonedDateTime created() {
            return null;
        }

        @Override
        public LocalDate start() {
            return start;
        }

        @Override
        public LocalDate end() {
            return end;
        }
    }
}
