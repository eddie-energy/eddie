package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.process.model.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import io.javalin.http.Context;
import io.javalin.validation.JavalinValidation;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static energy.eddie.regionconnector.es.datadis.utils.ParameterKeys.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatadisPermissionRequestTest {

    static Validator nullValidator = mock(Validator.class);

    @BeforeAll
    static void beforeAll() {
        JavalinValidation.register(ZonedDateTime.class, value -> value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(ZONE_ID_SPAIN) : null);
        JavalinValidation.register(MeasurementType.class, MeasurementType::valueOf);

        when(nullValidator.get()).thenReturn(null);
    }

    @Test
    void base_Constructor_Constructs() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);


        assertDoesNotThrow(() -> new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        ));
    }

    @Test
    void constructed_Request_IsInCreatedState() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);


        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertInstanceOf(CreatedPermissionRequestState.class, request.state());
    }


    @Test
    void constructor_WithoutPermissionId_GeneratesId() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);

        AuthorizationApi api = mock(AuthorizationApi.class);
        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertNotNull(request.permissionId());
        assertFalse(request.permissionId().isBlank());
    }

    @Test
    void constructor_WithoutConnectionId_TakesItFromContext() {
        var expectedConnectionId = "connectionId";
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        when(ctx.formParam(CONNECTION_ID_KEY)).thenReturn(expectedConnectionId);
        when(ctx.formParam(DATA_NEED_ID_KEY)).thenReturn("dataNeedId");
        AuthorizationApi api = mock(AuthorizationApi.class);
        DatadisPermissionRequest request = new DatadisPermissionRequest(
                ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expectedConnectionId, request.connectionId());
    }

    @Test
    void base_constructor_WithNullArguments_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(
                null, null, null, null, null
        ));
    }

    @Test
    void constructor_WithoutPermissionId_WithNullArguments_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(
                null, null, null
        ));
    }

    @Test
    void constructor_WithoutConnectionId_WithNullArguments_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(
                null, null, null
        ));
    }

    @Test
    void nif_Comes_From_Context() {
        var expectedNif = "nif";
        Context ctx = mock(Context.class);
        when(ctx.formParam(NIF_KEY)).thenReturn(expectedNif);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expectedNif, request.nif());
    }

    @Test
    void dataNeedId_Comes_From_Context() {
        var expectedDataNeedId = "dataNeedId";
        Context ctx = mock(Context.class);
        when(ctx.formParam(DATA_NEED_ID_KEY)).thenReturn(expectedDataNeedId);
        when(ctx.formParam(CONNECTION_ID_KEY)).thenReturn("connectionID");
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expectedDataNeedId, request.dataNeedId());
    }

    @Test
    void meteringPointId_Comes_From_Context() {
        var expectedMetringPointId = "meteringPointId";
        Context ctx = mock(Context.class);
        when(ctx.formParam(METERING_POINT_ID_KEY)).thenReturn(expectedMetringPointId);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expectedMetringPointId, request.meteringPointId());
    }

    @Test
    void requestDataFrom_Comes_From_Context() {
        var expected = ZonedDateTime.now(ZoneOffset.UTC);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        when(validator.getOrDefault(any())).thenReturn(expected);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        when(ctx.formParamAsClass(REQUEST_DATE_FROM_KEY, ZonedDateTime.class)).thenReturn(validator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expected, request.requestDataFrom());
    }

    @Test
    void requestDataTo_Comes_From_Context() {
        var expected = ZonedDateTime.now(ZoneOffset.UTC);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        when(validator.getOrDefault(any())).thenReturn(expected);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        when(ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class)).thenReturn(validator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expected, request.requestDataTo());
    }

    @Test
    void measurementType_Comes_From_Context() {
        var expected = MeasurementType.HOURLY;
        Context ctx = mock(Context.class);
        Validator<MeasurementType> validator = mock(Validator.class);
        when(validator.getOrDefault(any())).thenReturn(MeasurementType.HOURLY);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        when(ctx.formParamAsClass(MEASUREMENT_TYPE_KEY, MeasurementType.class)).thenReturn(validator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(expected, request.measurementType());
    }

    @Test
    void permissionEnd_whenRequestingFutureData_IsTheSameAsRequestDataTo() {
        var futureDate = ZonedDateTime.now(ZoneOffset.UTC).plusMonths(1);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        when(validator.getOrDefault(any())).thenReturn(futureDate);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        when(ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class)).thenReturn(validator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(request.requestDataTo(), request.permissionEnd());
    }

    @Test
    void permissionEnd_whenRequestingPastData_IsTheSameAsPermissionStart() {
        var pastDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        when(validator.getOrDefault(any())).thenReturn(pastDate);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        when(ctx.formParamAsClass(REQUEST_DATA_TO_KEY, ZonedDateTime.class)).thenReturn(validator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        DatadisPermissionRequest request = new DatadisPermissionRequest(
                "connectionId", "dataNeedId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertEquals(request.permissionStart(), request.permissionEnd());
    }

    @Test
    void lastPulledMeterReading_whenConstructed_IsEmpty() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);


        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertTrue(request.lastPulledMeterReading().isEmpty());
    }

    @Test
    void distributorCode_whenConstructed_IsEmpty() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);


        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertTrue(request.distributorCode().isEmpty());
    }

    @Test
    void pointType_whenConstructed_IsEmpty() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);


        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );

        assertTrue(request.pointType().isEmpty());
    }

    @Test
    void setLastPulledMeterReading_worksAsExpected() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        ZonedDateTime expected = ZonedDateTime.now(ZoneOffset.UTC);
        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );
        request.setLastPulledMeterReading(expected);

        assertEquals(expected, request.lastPulledMeterReading().get());
    }

    @Test
    void setDistributorCode_worksAsExpected() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        var expected = "distributorCode";
        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );
        request.setDistributorCode(expected);

        assertEquals(expected, request.distributorCode().get());
    }

    @Test
    void setPointType_worksAsExpected() {
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(anyString(), any())).thenReturn(nullValidator);
        AuthorizationApi api = mock(AuthorizationApi.class);

        var expected = 1;
        var request = new DatadisPermissionRequest(
                "permissionId", "connectionId", ctx, api, mock(AuthorizationResponseHandler.class)
        );
        request.setPointType(expected);

        assertEquals(expected, request.pointType().get());
    }
}