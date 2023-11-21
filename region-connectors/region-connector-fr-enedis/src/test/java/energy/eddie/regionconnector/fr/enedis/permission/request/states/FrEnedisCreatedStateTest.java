package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.shared.utils.ZonedDateTimeConverter;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrEnedisCreatedStateTest {

    @BeforeAll
    static void setUp() {
        ZonedDateTimeConverter.register();
    }

    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(CONNECTION_ID, String.class))
                .thenReturn(Validator.create(String.class, "cid", CONNECTION_ID));
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.plusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        EnedisConfiguration config = mock(EnedisConfiguration.class);

        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisValidatedState.class, permissionRequest.state().getClass());
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
        EnedisConfiguration config = mock(EnedisConfiguration.class);

        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
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
        EnedisConfiguration config = mock(EnedisConfiguration.class);

        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
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
        EnedisConfiguration config = mock(EnedisConfiguration.class);

        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
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
        EnedisConfiguration config = mock(EnedisConfiguration.class);

        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
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
        EnedisConfiguration config = mock(EnedisConfiguration.class);

        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, ctx, config);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }
}