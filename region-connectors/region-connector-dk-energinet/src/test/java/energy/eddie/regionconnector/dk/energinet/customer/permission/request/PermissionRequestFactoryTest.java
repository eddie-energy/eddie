package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import energy.eddie.regionconnector.dk.energinet.utils.PeriodResolutionEnumConverter;
import energy.eddie.regionconnector.shared.utils.ZonedDateTimeConverter;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.PERIOD_RESOLUTION_KEY;
import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.START_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRequestFactoryTest {
    @BeforeAll
    static void setUp() {
        ZonedDateTimeConverter.register();
        PeriodResolutionEnumConverter.register();
    }

    @Test
    void testCreatePermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EnerginetConfiguration conf = mock(EnerginetConfiguration.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, permissionStateMessages, conf);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(any(), eq(ZonedDateTime.class)))
                .thenReturn(new Validator<>(null, ZonedDateTime.class, START_KEY));
        when(ctx.formParamAsClass(PERIOD_RESOLUTION_KEY, PeriodResolutionEnum.class))
                .thenReturn(Validator.create(PeriodResolutionEnum.class, null, PERIOD_RESOLUTION_KEY));

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(ctx);

        // Then
        assertNotNull(permissionRequest);
    }
}
