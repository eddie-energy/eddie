package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerMalformedState;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnerginetCustomerPermissionRequestTest {
    @Test
    void constructorWithPermissionId_setsPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));

        // When
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(permissionId, connectionId, dataNeedId, ctx, configuration);

        // Then
        assertEquals(permissionId, request.permissionId());
    }

    @Test
    void constructorWithConnectionId_setsConnectionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));

        // When
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(permissionId, connectionId, dataNeedId, ctx, configuration);

        // Then
        assertEquals(connectionId, request.connectionId());
    }

    @Test
    void constructorWithoutPermissionId_generatesPermissionId() {
        // Given
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));

        // When
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(connectionId, dataNeedId, ctx, configuration);

        // Then
        assertNotNull(request.permissionId());
    }

    @Test
    void constructorWithContextStart_setsStart() {
        // Given
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        ZonedDateTime now = ZonedDateTime.now();
        when(validator.getOrDefault(any())).thenReturn(now);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));

        // When
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(connectionId, dataNeedId, ctx, configuration);

        // Then
        assertEquals(now, request.start());
    }

    @Test
    void constructorWithContextEnd_setsEnd() {
        // Given
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        Validator<ZonedDateTime> validator = mock(Validator.class);
        ZonedDateTime now = ZonedDateTime.now();
        when(validator.getOrDefault(any())).thenReturn(now);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));

        // When
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(connectionId, dataNeedId, ctx, configuration);

        // Then
        assertEquals(now, request.end());
    }

    @Test
    void constructorWithContextConnectionId_setConnectionId() {
        // Given
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParam(EnerginetCustomerPermissionRequest.CONNECTION_ID)).thenReturn("testConnectionId");
        Validator<ZonedDateTime> validator = mock(Validator.class);
        ZonedDateTime now = ZonedDateTime.now();
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(validator);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));

        // When
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(ctx, configuration);

        // Then
        assertEquals("testConnectionId", request.connectionId());
    }

    @Test
    void changeState_updatesState() {
        // Given
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration configuration = mock(EnerginetConfiguration.class);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.START_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.END_KEY, ZonedDateTime.class))
                .thenReturn(mock(Validator.class));
        when(ctx.formParamAsClass(EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(mock(Validator.class));
        EnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest(connectionId, dataNeedId, ctx, configuration);
        PermissionRequestState newState = new EnerginetCustomerMalformedState(request, Map.of());

        // When
        request.changeState(newState);

        // Then
        assertEquals(newState, request.state());
    }
}
