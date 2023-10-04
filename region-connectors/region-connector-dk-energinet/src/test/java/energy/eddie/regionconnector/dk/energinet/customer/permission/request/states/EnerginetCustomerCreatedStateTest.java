package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import energy.eddie.regionconnector.dk.energinet.utils.TimeSeriesAggregationEnumConverter;
import energy.eddie.regionconnector.dk.energinet.utils.ZonedDateTimeConverter;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnerginetCustomerCreatedStateTest {
    @BeforeAll
    static void setUp() {
        ZonedDateTimeConverter.register();
        TimeSeriesAggregationEnumConverter.register();
    }

    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenConnectionIdNull() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, null, CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenConnectionIdBlank() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenStartNull() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, null, START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndNull() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, null, END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.minusDays(1);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, start.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, end.format(DateTimeFormatter.ISO_DATE), END_KEY))
                .thenReturn(Validator.create(ZonedDateTime.class, end.format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenRefreshTokenBlank() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "meteringPoint", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenMeteringPointBlank() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        when(ctx.formParamAsClass(REFRESH_TOKEN_KEY, String.class))
                .thenReturn(Validator.create(String.class, "refreshToken", REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, "Actual", AGGREGATION_KEY));
        when(ctx.formParamAsClass(METERING_POINT_KEY, String.class))
                .thenReturn(Validator.create(String.class, "", METERING_POINT_KEY));
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        DkEnerginetCustomerPermissionRequest permissionRequest = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, config);
        EnerginetCustomerCreatedState createdState = new EnerginetCustomerCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }
}