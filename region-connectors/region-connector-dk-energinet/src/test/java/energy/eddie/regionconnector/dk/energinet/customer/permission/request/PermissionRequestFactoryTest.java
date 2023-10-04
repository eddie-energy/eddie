package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import energy.eddie.regionconnector.dk.energinet.utils.TimeSeriesAggregationEnumConverter;
import energy.eddie.regionconnector.dk.energinet.utils.ZonedDateTimeConverter;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.AGGREGATION_KEY;
import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.START_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionRequestFactoryTest {
    @BeforeAll
    static void setUp() {
        ZonedDateTimeConverter.register();
        TimeSeriesAggregationEnumConverter.register();
    }

    @Test
    void testCreatePermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EnerginetConfiguration conf = mock(EnerginetConfiguration.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, permissionStateMessages, conf);
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(any(), eq(ZonedDateTime.class)))
                .thenReturn(new Validator<>(null, ZonedDateTime.class, START_KEY));
        when(ctx.formParamAsClass(AGGREGATION_KEY, TimeSeriesAggregationEnum.class))
                .thenReturn(Validator.create(TimeSeriesAggregationEnum.class, null, AGGREGATION_KEY));

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(ctx);

        // Then
        assertNotNull(permissionRequest);
    }
}
