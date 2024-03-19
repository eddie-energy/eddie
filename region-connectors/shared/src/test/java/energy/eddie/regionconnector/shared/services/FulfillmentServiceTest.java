package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.RegionConnectorMetadata;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("NullAway")
class FulfillmentServiceTest {

    ZonedDateTime today = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
    RegionConnectorMetadata regionConnectorMetadata = new SimpleMetaData();
    FulfillmentService<SimplePermissionRequest> fulfillmentService = new FulfillmentService<>(regionConnectorMetadata);


    @Test
    void isPermissionRequestFulfilledByDate_dateBeforePermissionEndDate_returnsFalse() throws StateTransitionException {
        // Given
        ZonedDateTime start = today.minusDays(2);
        ZonedDateTime end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        boolean isFulfilledBy = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, today);

        // Then
        assertFalse(isFulfilledBy);
    }

    @Test
    void isPermissionRequestFulfilledByDate_dateEqualPermissionEndDate_returnsFalse() throws StateTransitionException {
        // Given
        ZonedDateTime start = today.minusDays(2);
        ZonedDateTime end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        boolean isFulfilledBy = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, end);

        // Then
        assertFalse(isFulfilledBy);
    }

    @Test
    void isPermissionRequestFulfilledByDate_dateAfterPermissionEndDate_returnsTrue() throws StateTransitionException {
        // Given
        ZonedDateTime start = today.minusDays(2);
        ZonedDateTime end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        boolean isFulfilledBy = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, end.plusDays(1));

        // Then
        assertTrue(isFulfilledBy);
    }

    @Test
    void tryFulfillPermissionRequest_changesStateToFulfilled() {
        // Given
        ZonedDateTime start = today.minusDays(2);
        ZonedDateTime end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.state().status());
    }

    @Test
    void tryFulfillPermissionRequest_doesNothingOnException() {
        // Given
        ZonedDateTime start = today.minusDays(2);
        ZonedDateTime end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end, true);

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertNull(permissionRequest.state());
    }

    private static class SimpleMetaData implements RegionConnectorMetadata {

        @Override
        public String id() {
            return "id";
        }

        @Override
        public String countryCode() {
            return "countryCode";
        }

        @Override
        public long coveredMeteringPoints() {
            return 0;
        }
    }

    private static class SimplePermissionRequest implements TimeframedPermissionRequest {

        private final ZonedDateTime start;
        private final ZonedDateTime end;
        private final boolean throwsOnFulfill;
        private PermissionRequestState state = null;

        private SimplePermissionRequest(ZonedDateTime start, ZonedDateTime end) {
            this.start = start;
            this.end = end;
            this.throwsOnFulfill = false;
        }

        private SimplePermissionRequest(ZonedDateTime start, ZonedDateTime end, boolean throwsOnFulfill) {
            this.start = start;
            this.end = end;
            this.throwsOnFulfill = throwsOnFulfill;
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
        public void fulfill() throws StateTransitionException {
            if (throwsOnFulfill) {
                throw new PastStateException(SentToPermissionAdministratorPermissionRequestState.class);
            }
            state = new FulfilledPermissionRequestState() {
            };
        }

        @Override
        public DataSourceInformation dataSourceInformation() {
            return null;
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
        public ZonedDateTime start() {
            return start;
        }

        @Nullable
        @Override
        public ZonedDateTime end() {
            return end;
        }
    }
}
