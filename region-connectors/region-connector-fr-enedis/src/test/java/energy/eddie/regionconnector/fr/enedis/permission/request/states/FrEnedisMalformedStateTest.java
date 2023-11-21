package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import io.javalin.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisMalformedStateTest {

    @Test
    void toString_returnsErrorString() {
        // Given
        TimeframedPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");
        Map<String, List<ValidationError<?>>> errors = new HashMap<>();
        List<ValidationError<?>> errorList = new ArrayList<>();
        errorList.add(new ValidationError<>("Error message", Map.of("field", "errorField"), null));
        errors.put("key", errorList);
        FrEnedisMalformedState malformedState = new FrEnedisMalformedState(permissionRequest, errors);

        // When
        String toStringResult = malformedState.toString();

        // Then
        String expectedToString = "MalformedState{errors={key=[ValidationError(message=Error message, args={field=errorField}, value=null)]}}";
        assertEquals(expectedToString, toStringResult);
    }
}