package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.permission.requests.extensions.DummyDataSourceInformation;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeterReadingPermissionRequestUpdateAndFulfillmentServiceTest {

    private static final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    public static Stream<Arguments> latestMeterReadingEndDates() {
        return Stream.of(
                Arguments.of((LocalDate) null),
                Arguments.of(today.minusDays(1)),
                Arguments.of(today),
                Arguments.of(today.plusDays(1))
        );
    }

    @ParameterizedTest
    @MethodSource("latestMeterReadingEndDates")
    void tryUpdateAndFulfillPermissionRequest_withMeterReadingEndSameAsPermission_LatestAsExpectedButDoesNotFulfill(@Nullable LocalDate latest) {
        // Given
        LocalDate end = today.plusDays(1);
        MeterReadingPermissionRequest permissionRequest = acceptedPermissionRequest(end);
        permissionRequest.updateLatestMeterReadingEndDate(latest);

        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService());

        // When
        service.tryUpdateAndFulfillPermissionRequest(permissionRequest, () -> end);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(end), permissionRequest.latestMeterReadingEndDate()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status())
        );
    }

    private static MeterReadingPermissionRequest acceptedPermissionRequest(LocalDate end) {
        var permissionRequest = new SimpleMeterReadingPermissionRequest(
                MeterReadingPermissionRequestUpdateAndFulfillmentServiceTest.today, end);
        permissionRequest.changeState(new SimpleAcceptedPermissionRequestState(permissionRequest));

        return permissionRequest;
    }

    @Test
    void tryUpdateAndFulfillPermissionRequest_withMeterReadingEndBeforeLatest_DoesNotUpdateLatestAndDoesNotFulfill() {
        // Given
        LocalDate latest = today.minusDays(2);
        MeterReadingPermissionRequest permissionRequest = acceptedPermissionRequest(latest);
        permissionRequest.updateLatestMeterReadingEndDate(latest);

        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService());

        // When
        service.tryUpdateAndFulfillPermissionRequest(permissionRequest, () -> latest.minusDays(1));

        // Then
        assertAll(
                () -> assertEquals(Optional.of(latest), permissionRequest.latestMeterReadingEndDate()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status())
        );
    }

    @Test
    void tryUpdateAndFulfillPermissionRequest_withMeterReadingEndAfterPermission_UpdatesLatestANdFulfills() {
        // Given
        LocalDate end = today.plusDays(1);
        MeterReadingPermissionRequest permissionRequest = acceptedPermissionRequest(today);

        MeterReadingPermissionUpdateAndFulfillmentService service = new MeterReadingPermissionUpdateAndFulfillmentService(
                new FulfillmentService());

        // When
        service.tryUpdateAndFulfillPermissionRequest(permissionRequest, () -> end);

        // Then
        assertAll(
                () -> assertEquals(Optional.of(end), permissionRequest.latestMeterReadingEndDate()),
                () -> assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.state().status())
        );
    }

    private record SimpleAcceptedPermissionRequestState(
            SimpleMeterReadingPermissionRequest permissionRequest) implements AcceptedPermissionRequestState {

        @Override
        public void terminate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void revoke() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fulfill() {
            permissionRequest.changeState(new FulfilledPermissionRequestState() {
            });
        }
    }


    private static class SimpleMeterReadingPermissionRequest implements MeterReadingPermissionRequest {
        private final LocalDate start;
        private final LocalDate end;
        @Nullable
        private LocalDate latestMeterReadingEndDate = null;
        @Nullable
        private PermissionRequestState state = null;

        public SimpleMeterReadingPermissionRequest(
                LocalDate start,
                LocalDate end
        ) {
            this.start = start;
            this.end = end;
        }

        @Override
        public Optional<LocalDate> latestMeterReadingEndDate() {
            return Optional.ofNullable(latestMeterReadingEndDate);
        }

        @Override
        public void updateLatestMeterReadingEndDate(LocalDate date) {
            latestMeterReadingEndDate = date;
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
        public PermissionRequestState state() {
            return state;
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
        public void changeState(PermissionRequestState state) {
            this.state = state;
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
