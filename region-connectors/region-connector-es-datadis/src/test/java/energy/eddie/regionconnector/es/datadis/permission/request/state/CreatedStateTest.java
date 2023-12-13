package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import io.javalin.http.Context;
import io.javalin.validation.JavalinValidation;
import io.javalin.validation.ValidationError;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static energy.eddie.regionconnector.es.datadis.utils.ParameterKeys.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

class CreatedStateTest {
    AuthorizationApi authorizationApi = mock(AuthorizationApi.class);
    AuthorizationResponseHandler authorizationResponseHandler = mock(AuthorizationResponseHandler.class);

    @BeforeAll
    static void setUp() {
        JavalinValidation.register(ZonedDateTime.class, value -> value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(ZONE_ID_SPAIN) : null);
        JavalinValidation.register(MeasurementType.class, MeasurementType::valueOf);

    }

    private static Stream<Arguments> invalidParameterProvider() {
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        return Stream.of(
                arguments(null, "nif", "meteringPoint", now, now.plusDays(1), MeasurementType.HOURLY, "connectionId must not be null"),
                arguments("", "nif", "meteringPoint", now, now.plusDays(1), MeasurementType.HOURLY, "connectionId must not be blank"),
                arguments("cid", null, "meteringPoint", now, now.plusDays(1), MeasurementType.HOURLY, "nif must not be null"),
                arguments("cid", "", "meteringPoint", now, now.plusDays(1), MeasurementType.HOURLY, "nif must not be blank"),
                arguments("cid", "nif", null, now, now.plusDays(1), MeasurementType.HOURLY, "meteringPointId must not be null"),
                arguments("cid", "nif", "", now, now.plusDays(1), MeasurementType.HOURLY, "meteringPointId must not be blank"),
                arguments("cid", "nif", "meteringPoint", null, now.plusDays(1), MeasurementType.HOURLY, "start must not be null"),
                arguments("cid", "nif", "meteringPoint", now.minusMonths(MAXIMUM_MONTHS_IN_THE_PAST), now.plusDays(1), MeasurementType.HOURLY, "start must not be earlier than " + MAXIMUM_MONTHS_IN_THE_PAST + " months"),
                arguments("cid", "nif", "meteringPoint", now, null, MeasurementType.HOURLY, "end must not be null"),
                arguments("cid", "nif", "meteringPoint", now, now.minusDays(1), MeasurementType.HOURLY, "end must not be before start"),
                arguments("cid", "nif", "meteringPoint", now, now.plusDays(1), null, "measurementType must not be null")
        );
    }

    private static void setupContextMock(Context ctx) {
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        when(ctx.formParam(NIF_KEY)).thenReturn("nif");
        when(ctx.formParam(METERING_POINT_ID_KEY)).thenReturn("meteringPoint");

        when(ctx.formParamAsClass(CONNECTION_ID_KEY, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID_KEY));
        when(ctx.formParamAsClass(NIF_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", NIF_KEY));
        when(ctx.formParamAsClass(METERING_POINT_ID_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_ID_KEY));
        when(ctx.formParamAsClass(MEASUREMENT_TYPE_KEY, MeasurementType.class))
                .thenReturn(Validator.create(MeasurementType.class, MeasurementType.HOURLY.name(), MEASUREMENT_TYPE_KEY));

        when(ctx.formParamAsClass(REQUEST_DATE_FROM_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), REQUEST_DATE_FROM_KEY));
        when(ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), REQUEST_DATA_TO_KEY));
    }

    @ParameterizedTest(name = "{6}")
    @MethodSource("invalidParameterProvider")
    void validate_changesToMalformedState_whenInvalidParameter(
            String connectionId,
            String nif,
            String meteringPoint,
            ZonedDateTime requestDateFrom,
            ZonedDateTime requestDataTo,
            MeasurementType measurementType,
            String displayName
    ) {
        // Given
        Context ctx = mock(Context.class);
        when(ctx.formParam(NIF_KEY)).thenReturn("nif");
        when(ctx.formParam(METERING_POINT_ID_KEY)).thenReturn("meteringPoint");

        when(ctx.formParamAsClass(CONNECTION_ID_KEY, String.class))
                .thenReturn(Validator.create(String.class, connectionId, CONNECTION_ID_KEY));
        when(ctx.formParamAsClass(NIF_KEY, String.class))
                .thenReturn(Validator.create(String.class, nif, NIF_KEY));
        when(ctx.formParamAsClass(METERING_POINT_ID_KEY, String.class))
                .thenReturn(Validator.create(String.class, meteringPoint, METERING_POINT_ID_KEY));
        when(ctx.formParamAsClass(MEASUREMENT_TYPE_KEY, MeasurementType.class))
                .thenReturn(Validator.create(MeasurementType.class, measurementType == null ? null : measurementType.name(), MEASUREMENT_TYPE_KEY));

        when(ctx.formParamAsClass(REQUEST_DATE_FROM_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, requestDateFrom == null ? null : requestDateFrom.format(DateTimeFormatter.ISO_DATE), REQUEST_DATE_FROM_KEY))
                .thenReturn(Validator.create(ZonedDateTime.class, requestDateFrom == null ? null : requestDateFrom.format(DateTimeFormatter.ISO_DATE), REQUEST_DATE_FROM_KEY));
        when(ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, requestDataTo == null ? null : requestDataTo.format(DateTimeFormatter.ISO_DATE), REQUEST_DATA_TO_KEY))
                .thenReturn(Validator.create(ZonedDateTime.class, requestDataTo == null ? null : requestDataTo.format(DateTimeFormatter.ISO_DATE), REQUEST_DATA_TO_KEY));

        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", "cid", ctx, authorizationApi, authorizationResponseHandler);
        CreatedState createdState = new CreatedState(permissionRequest, ctx, authorizationApi, authorizationResponseHandler);

        // When
        createdState.validate();

        // Then
        assertEquals(MalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        Context ctx = mock(Context.class);
        setupContextMock(ctx);

        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", "cid", ctx, authorizationApi, authorizationResponseHandler);
        CreatedState createdState = new CreatedState(permissionRequest, ctx, authorizationApi, authorizationResponseHandler);

        // When
        createdState.validate();

        // Then
        assertEquals(ValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_reportsAllErrors_whenEverythingIsNull() {
        // Given
        Context ctx = mock(Context.class);
        when(ctx.formParam(NIF_KEY)).thenReturn("nif");
        when(ctx.formParam(METERING_POINT_ID_KEY)).thenReturn("meteringPoint");

        when(ctx.formParamAsClass(CONNECTION_ID_KEY, String.class))
                .thenReturn(Validator.create(String.class, null, CONNECTION_ID_KEY));
        when(ctx.formParamAsClass(NIF_KEY, String.class))
                .thenReturn(Validator.create(String.class, null, NIF_KEY));
        when(ctx.formParamAsClass(METERING_POINT_ID_KEY, String.class))
                .thenReturn(Validator.create(String.class, null, METERING_POINT_ID_KEY));
        when(ctx.formParamAsClass(MEASUREMENT_TYPE_KEY, MeasurementType.class))
                .thenReturn(Validator.create(MeasurementType.class, null, MEASUREMENT_TYPE_KEY));

        when(ctx.formParamAsClass(REQUEST_DATE_FROM_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, null, REQUEST_DATE_FROM_KEY));
        when(ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, null, REQUEST_DATA_TO_KEY))
                .thenReturn(Validator.create(ZonedDateTime.class, null, REQUEST_DATA_TO_KEY));

        DatadisPermissionRequest permissionRequest = new DatadisPermissionRequest("pid", "cid", ctx, authorizationApi, authorizationResponseHandler);
        CreatedState createdState = new CreatedState(permissionRequest, ctx, authorizationApi, authorizationResponseHandler);

        // When
        createdState.validate();


        // Then
        ArgumentCaptor<Map<String, List<ValidationError<Object>>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        assertEquals(6, captor.getValue().size());
    }

}