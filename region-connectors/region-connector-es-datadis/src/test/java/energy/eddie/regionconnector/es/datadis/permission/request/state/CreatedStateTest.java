package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class CreatedStateTest {
    private final AuthorizationApi authorizationApi = mock(AuthorizationApi.class);

    private static Stream<Arguments> invalidParameterProvider() {
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        return Stream.of(
                arguments(now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST), now.minusDays(10), "requestDataFrom must not be older than " + MAXIMUM_MONTHS_IN_THE_PAST + " months"),
                arguments(now.minusDays(10), now.minusDays(20), "requestDataFrom must be before requestDataTo")
        );
    }

    private static Stream<Arguments> validParameterProvider() {
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        return Stream.of(
                arguments(now.minusDays(2), now.minusDays(1), "start before end"),
                arguments(now.minusDays(1), now.minusDays(1), "start equals end"),
                arguments(now.minusDays(1), now.plusDays(1), "start in the past, end in the future")
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("invalidParameterProvider")
    void validate_changesToMalformedState_whenInvalidParameter(
            ZonedDateTime requestDataFrom,
            ZonedDateTime requestDataTo,
            String expectedErrorMessage) {
        // Given
        var permissionId = "foo";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, authorizationApi);

        CreatedState createdState = new CreatedState(permissionRequest, authorizationApi);

        // When
        var thrown = assertThrows(ValidationException.class, createdState::validate);
        assertThat(thrown.getMessage()).containsIgnoringCase(expectedErrorMessage);

        // Then
        assertEquals(MalformedState.class, permissionRequest.state().getClass());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validParameterProvider")
    void validate_changesToValidatedState_whenValid(ZonedDateTime requestDataFrom, ZonedDateTime requestDataTo, String testName) {
        // Given
        var permissionId = "foo";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";

        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, authorizationApi);
        CreatedState createdState = new CreatedState(permissionRequest, authorizationApi);

        // When
        assertDoesNotThrow(createdState::validate);

        // Then
        assertEquals(ValidatedState.class, permissionRequest.state().getClass());
    }
}