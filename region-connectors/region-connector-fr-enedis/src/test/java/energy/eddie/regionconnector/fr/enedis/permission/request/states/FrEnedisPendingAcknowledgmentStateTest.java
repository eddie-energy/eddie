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

import static energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest.END_KEY;
import static energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest.START_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FrEnedisPendingAcknowledgmentStateTest {

    @BeforeAll
    static void setUp() {
        ZonedDateTimeConverter.register();
    }

    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(START_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.format(DateTimeFormatter.ISO_DATE), START_KEY));
        when(ctx.formParamAsClass(END_KEY, ZonedDateTime.class))
                .thenReturn(Validator.create(ZonedDateTime.class, now.minusDays(1).format(DateTimeFormatter.ISO_DATE), END_KEY));
        EnedisConfiguration config = mock(EnedisConfiguration.class);
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", ctx, config);
        FrEnedisPendingAcknowledgmentState state = new FrEnedisPendingAcknowledgmentState(permissionRequest);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(FrEnedisSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }
}