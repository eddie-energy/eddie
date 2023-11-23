package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisMalformedState;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisPermissionRequestTest {
    @Test
    @SuppressWarnings("unchecked")
    void constructorWithPermissionId_setsPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, ctx, configuration);

        // Then
        assertEquals(permissionId, request.permissionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void constructorWithConnectionId_setsConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(permissionId, connectionId, ctx, configuration);

        // Then
        assertEquals(connectionId, request.connectionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void constructorWithoutPermissionId_generatesPermissionId() {
        // Given
        String connectionId = "testConnectionId";
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(connectionId, ctx, configuration);

        // Then
        assertNotNull(request.permissionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void constructorWithContextStart_setsStart() {
        // Given
        String connectionId = "testConnectionId";
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        when(validator.getOrDefault(any())).thenReturn(now);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(validator);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(connectionId, ctx, configuration);

        // Then
        assertEquals(now, request.start());
    }

    @Test
    @SuppressWarnings("unchecked")
    void constructorWithContextEnd_setsEnd() {
        // Given
        String connectionId = "testConnectionId";
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        when(validator.getOrDefault(any())).thenReturn(now);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(validator);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(connectionId, ctx, configuration);

        // Then
        assertEquals(now, request.end());
    }

    @Test
    @SuppressWarnings("unchecked")
    void constructorWithContextConnectionId_setConnectionId() {
        // Given
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParam(EnedisPermissionRequest.CONNECTION_ID)).thenReturn("testConnectionId");
        Validator<ZonedDateTime> validator = mock(Validator.class);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(validator);

        // When
        EnedisPermissionRequest request = new EnedisPermissionRequest(ctx, configuration);

        // Then
        assertEquals("testConnectionId", request.connectionId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void changeState_updatesState() {
        // Given
        EnedisConfiguration configuration = mock(EnedisConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnedisPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnedisPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        EnedisPermissionRequest request = new EnedisPermissionRequest("testConnectionId", ctx, configuration);
        PermissionRequestState newState = new FrEnedisMalformedState(request, Map.of());

        // When
        request.changeState(newState);

        // Then
        assertEquals(newState, request.state());
    }
}