package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import io.javalin.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnerginetCustomerMalformedStateTest {
    @Test
    void toString_returnsErrorString() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");
        Map<String, List<ValidationError<?>>> errors = new HashMap<>();
        List<ValidationError<?>> errorList = new ArrayList<>();
        errorList.add(new ValidationError<>("Error message", Map.of("field", "errorField"), null));
        errors.put("key", errorList);
        EnerginetCustomerMalformedState malformedState = new EnerginetCustomerMalformedState(permissionRequest, errors);

        // When
        String toStringResult = malformedState.toString();

        // Then
        String expectedToString = "MalformedState{errors={key=[ValidationError(message=Error message, args={field=errorField}, value=null)]}}";
        assertEquals(expectedToString, toStringResult);
    }
}
