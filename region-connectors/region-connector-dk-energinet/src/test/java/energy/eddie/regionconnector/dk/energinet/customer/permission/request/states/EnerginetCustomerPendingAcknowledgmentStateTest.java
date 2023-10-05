package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.utils.ZonedDateTimeConverter;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import energy.eddie.regionconnector.dk.energinet.utils.TimeSeriesAggregationEnumConverter;
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

class EnerginetCustomerPendingAcknowledgmentStateTest {
    @BeforeAll
    static void setUp() {
        ZonedDateTimeConverter.register();
        TimeSeriesAggregationEnumConverter.register();
    }

    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
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
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(permissionRequest);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(EnerginetCustomerSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }
}
