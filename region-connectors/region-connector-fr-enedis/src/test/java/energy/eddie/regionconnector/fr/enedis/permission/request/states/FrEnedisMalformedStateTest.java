package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrEnedisMalformedStateTest {

    @Test
    void toString_returnsErrorString() {
        // Given
        FrEnedisPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");
        List<AttributeError> errors = List.of(new AttributeError("field", "Error message"));
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisCreatedState state = new FrEnedisCreatedState(permissionRequest, factory);
        ValidationException exception = new ValidationException(state, errors);
        FrEnedisMalformedState malformedState = new FrEnedisMalformedState(permissionRequest, exception);

        // When
        String toStringResult = malformedState.toString();

        // Then
        assertThat(toStringResult)
                .startsWith("MalformedState{cause=")
                .contains("Validation errors in class ");
    }
}