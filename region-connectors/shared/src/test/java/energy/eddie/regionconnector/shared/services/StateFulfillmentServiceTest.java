package energy.eddie.regionconnector.shared.services;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.SentToPermissionAdministratorPermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("NullAway")
class StateFulfillmentServiceTest {

    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    FulfillmentService fulfillmentService = new StateFulfillmentService();


    @Test
    void isPermissionRequestFulfilledByDate_dateBeforePermissionEndDate_returnsFalse() {
        // Given
        LocalDate start = today.minusDays(2);
        LocalDate end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        boolean isFulfilledBy = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, today);

        // Then
        assertFalse(isFulfilledBy);
    }

    @Test
    void isPermissionRequestFulfilledByDate_dateEqualPermissionEndDate_returnsFalse() {
        // Given
        LocalDate start = today.minusDays(2);
        LocalDate end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        boolean isFulfilledBy = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, end);

        // Then
        assertFalse(isFulfilledBy);
    }

    @Test
    void isPermissionRequestFulfilledByDate_dateAfterPermissionEndDate_returnsTrue() {
        // Given
        LocalDate start = today.minusDays(2);
        LocalDate end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        boolean isFulfilledBy = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest,
                                                                                      end.plusDays(1));

        // Then
        assertTrue(isFulfilledBy);
    }

    @Test
    void tryFulfillPermissionRequest_changesStateToFulfilled() {
        // Given
        LocalDate start = today.minusDays(2);
        LocalDate end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end);

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.state().status());
    }

    @Test
    void tryFulfillPermissionRequest_doesNothingOnException() {
        // Given
        LocalDate start = today.minusDays(2);
        LocalDate end = today.plusDays(1);
        SimplePermissionRequest permissionRequest = new SimplePermissionRequest(start, end, true);

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertNull(permissionRequest.state());
    }

    private static class SimplePermissionRequest implements PermissionRequest {

        private final LocalDate start;
        private final LocalDate end;
        private final boolean throwsOnFulfill;
        private PermissionRequestState state = null;

        private SimplePermissionRequest(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
            this.throwsOnFulfill = false;
        }

        private SimplePermissionRequest(LocalDate start, LocalDate end, boolean throwsOnFulfill) {
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
        public DataSourceInformation dataSourceInformation() {
            return new DataSourceInformation() {
                @Override
                public String countryCode() {
                    return "countryCode";
                }

                @Override
                public String regionConnectorId() {
                    return "regionConnectorId";
                }

                @Override
                public String meteredDataAdministratorId() {
                    return "meteredDataAdministratorId";
                }

                @Override
                public String permissionAdministratorId() {
                    return "permissionAdministratorId";
                }
            };
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
        public void fulfill() throws StateTransitionException {
            if (throwsOnFulfill) {
                throw new PastStateException(SentToPermissionAdministratorPermissionRequestState.class);
            }
            state = new FulfilledPermissionRequestState() {
            };
        }

        @Override
        public LocalDate start() {
            return start;
        }

        @Nullable
        @Override
        public LocalDate end() {
            return end;
        }
    }
}
