package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.DataSourceInformation;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartIsBeforeOrEqualEndValidatorTest {
    @Test
    void testValidateWhen_startIsBeforeEnd() {
        // Given
        ZonedDateTime startTime = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime endTime = startTime.plusHours(1);
        TimeframedPermissionRequest request = new SimpleTimeFramePermissionRequest(startTime, endTime);
        StartIsBeforeOrEqualEndValidator<TimeframedPermissionRequest> validator = new StartIsBeforeOrEqualEndValidator<>();

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWhen_startIsEqualToEnd() {
        // Given
        ZonedDateTime startTime = ZonedDateTime.now(ZoneOffset.UTC);
        TimeframedPermissionRequest request = new SimpleTimeFramePermissionRequest(startTime, startTime);
        StartIsBeforeOrEqualEndValidator<TimeframedPermissionRequest> validator = new StartIsBeforeOrEqualEndValidator<>();

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWhen_startIsAfterEnd() {
        // Given
        ZonedDateTime startTime = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime endTime = startTime.minusHours(1);
        TimeframedPermissionRequest request = new SimpleTimeFramePermissionRequest(startTime, endTime);
        StartIsBeforeOrEqualEndValidator<TimeframedPermissionRequest> validator = new StartIsBeforeOrEqualEndValidator<>();

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertFalse(errors.isEmpty());
    }

    private record SimpleTimeFramePermissionRequest(ZonedDateTime start,
                                                    ZonedDateTime end) implements TimeframedPermissionRequest {

        @Override
        public String permissionId() {
            return null;
        }

        @Override
        public String connectionId() {
            return null;
        }

        @Override
        public String dataNeedId() {
            return null;
        }

        @Override
        public PermissionRequestState state() {
            return null;
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
            // No-Op
        }
    }
}