package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisMalformedStateTest {

    @Test
    void toString_returnsErrorString() {
        // Given
        TimeframedPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");
        List<AttributeError> errors = List.of(new AttributeError("field", "Error message"));
        FrEnedisMalformedState malformedState = new FrEnedisMalformedState(permissionRequest, errors);

        // When
        String toStringResult = malformedState.toString();

        // Then
        String expectedToString = "MalformedState{errors=[AttributeError[name=field, message=Error message]]}";
        assertEquals(expectedToString, toStringResult);
    }
}