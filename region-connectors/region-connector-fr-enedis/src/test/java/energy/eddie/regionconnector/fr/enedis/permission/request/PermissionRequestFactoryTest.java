package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest.START_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EnedisConfiguration conf = mock(EnedisConfiguration.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, permissionStateMessages, conf);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(any(), eq(ZonedDateTime.class)))
                .thenReturn(new Validator<>(null, ZonedDateTime.class, START_KEY));

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(ctx);

        // Then
        assertNotNull(permissionRequest);
    }
}