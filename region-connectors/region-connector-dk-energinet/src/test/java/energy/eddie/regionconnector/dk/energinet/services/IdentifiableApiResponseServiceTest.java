package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapitools.jackson.nullable.JsonNullableModule;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Objects;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.mockito.Mockito.*;

@SuppressWarnings("DataFlowIssue") // suppress null pointer warning
class IdentifiableApiResponseServiceTest {

    private final static ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new Jdk8Module()).registerModule(new JsonNullableModule());
    private final MyEnergyDataMarketDocumentResponse marketDocumentResponse = readMyEnergyDataMarketDocumentResponseListApiResponse();
    private final ZonedDateTime periodEnd = ZonedDateTime.parse(marketDocumentResponse.getMyEnergyDataMarketDocument().getPeriodTimeInterval().getEnd()).withZoneSameInstant(DK_ZONE_ID);
    private final ZonedDateTime periodStart = ZonedDateTime.parse(marketDocumentResponse.getMyEnergyDataMarketDocument().getPeriodTimeInterval().getStart()).withZoneSameInstant(DK_ZONE_ID);

    IdentifiableApiResponseServiceTest() throws IOException {
    }

    public static MyEnergyDataMarketDocumentResponse readMyEnergyDataMarketDocumentResponseListApiResponse() throws IOException {
        try (InputStream is = IdentifiableApiResponseServiceTest.class.getClassLoader().getResourceAsStream("MyEnergyDataMarketDocumentResponseListApiResponse.json")) {
            return objectMapper.readValue(Objects.requireNonNull(is), MyEnergyDataMarketDocumentResponseListApiResponse.class).getResult().getFirst();
        }
    }

    @Test
    void ifLastPolledEqualsStart_callsUpdateLastPolled() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest(periodStart, periodEnd, periodStart);
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        // When
        new IdentifiableApiResponseService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableApiResponse(spy, marketDocumentResponse)))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(spy).updateLastPolled(periodEnd);
    }

    @Test
    void ifLastPolledBeforeEndOfPeriod_callsUpdateLastPolled() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest(periodStart, periodEnd, periodEnd.minusDays(1));
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        // When
        new IdentifiableApiResponseService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableApiResponse(spy, marketDocumentResponse)))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(spy).updateLastPolled(periodEnd);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void ifLastPolledEqualOrAfterEndOfPeriod_doesNotCallUpdateLastPolled(int days) {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest(periodStart, periodEnd, periodEnd.plusDays(days));
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        // When
        new IdentifiableApiResponseService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableApiResponse(spy, marketDocumentResponse)))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(spy, never()).updateLastPolled(any());
    }

    @Test
    void periodEndBeforePermissionEndDate_doesNotCallFulfill() throws StateTransitionException {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest(periodStart, periodEnd.plusDays(1), periodEnd.minusDays(1));
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        // When
        new IdentifiableApiResponseService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableApiResponse(spy, marketDocumentResponse)))
                .expectNextCount(1)
                .verifyComplete();
        // Then
        verify(spy, never()).fulfill();
    }

    @Test
    void periodEndAfterPermissionEndDate_callsFulfillOnlyOnce() throws StateTransitionException {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest(periodStart, periodEnd.minusDays(1), periodEnd.minusDays(1));
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);
        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        // When
        new IdentifiableApiResponseService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> {
                    testPublisher.emit(new IdentifiableApiResponse(spy, marketDocumentResponse), new IdentifiableApiResponse(spy, marketDocumentResponse));
                    testPublisher.complete();
                })
                .expectNextCount(2)
                .verifyComplete();

        // Then
        verify(spy, times(1)).fulfill();
    }


    @Test
    void periodEndEqualsPermissionEndDate_doesNotCallFulfill() throws StateTransitionException {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest(periodStart, periodEnd, periodEnd.minusDays(1));
        DkEnerginetCustomerPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableApiResponse> testPublisher = TestPublisher.create();

        // When
        new IdentifiableApiResponseService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableApiResponse(spy, marketDocumentResponse)))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(spy, never()).fulfill();
    }
}
