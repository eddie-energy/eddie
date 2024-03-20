package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.io.IOException;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdentifiableMeterReadingServiceTest {
    @Test
    void ifLatestMeterReadingEmpty_callsSetLastPulledMeterReading() throws IOException {
        // Arrange
        FrEnedisPermissionRequest permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.latestMeterReading()).thenReturn(Optional.empty());

        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);
        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeterReading(permissionRequest, meterReading)))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).updateLatestMeterReading(meterReading.end());
    }

    @Test
    void ifLatestMeterReadingPresent_callsSetLastPulledMeterReading_ifMeterReadingNewer() throws IOException {
        // Arrange
        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);

        FrEnedisPermissionRequest permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.latestMeterReading()).thenReturn(Optional.of(meterReading.end().minusDays(1)));

        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeterReading(permissionRequest, meterReading)))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest).updateLatestMeterReading(meterReading.end());
    }

    @Test
    void ifLatestMeterReadingPresent_doesNotCallSetLastPulledMeterReading_ifMeterReadingOlder() throws IOException {
        // Arrange
        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);

        FrEnedisPermissionRequest permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.latestMeterReading()).thenReturn(Optional.of(meterReading.end().plusDays(1)));

        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeterReading(permissionRequest, meterReading)))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest, never()).updateLatestMeterReading(any());
    }

    @Test
    void meterReadingEndDateBeforePermissionEndDate_doesNotCallFulfill() throws StateTransitionException, IOException {
        // Arrange
        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);

        FrEnedisPermissionRequest permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.end()).thenReturn(meterReading.end().plusDays(1).atStartOfDay(ZONE_ID_FR));

        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeterReading(permissionRequest, meterReading)))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(permissionRequest, never()).fulfill();
    }

    @Test
    void meterReadingEndDateAfterPermissionEndDate_callsFulfillOnlyOnce() throws StateTransitionException, IOException {
        // Arrange
        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pId", "cId", "dId",
                meterReading.start().atStartOfDay(ZONE_ID_FR),
                meterReading.end().minusDays(1).atStartOfDay(ZONE_ID_FR),
                Granularity.P1D,
                factory);
        permissionRequest.changeState(factory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build());
        FrEnedisPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> {
                    testPublisher.emit(new IdentifiableMeterReading(permissionRequest, meterReading), new IdentifiableMeterReading(spy, meterReading));
                    testPublisher.complete();
                })
                .expectNextCount(2)
                .verifyComplete();
        // Assert
        verify(spy, times(1)).fulfill();
    }

    @Test
    void meterReadingEndDateAfterPermissionEndDate_StateIsNotAccepted_ThrowsAndDoesNotChangeState() throws StateTransitionException, IOException {
        // Arrange
        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pId", "cId", "dId",
                meterReading.start().atStartOfDay(ZONE_ID_FR),
                meterReading.end().minusDays(1).atStartOfDay(ZONE_ID_FR),
                Granularity.P1D,
                factory);
        permissionRequest.changeState(factory.create(permissionRequest, PermissionProcessStatus.REVOKED).build());
        FrEnedisPermissionRequest spy = spy(permissionRequest);

        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeterReading(spy, meterReading)))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(spy, times(1)).fulfill();
        assertThrows(StateTransitionException.class, spy::fulfill);
        assertEquals(PermissionProcessStatus.REVOKED, spy.status());
    }

    @Test
    void meterReadingEndDateEqualsPermissionEndDate_doesNotCallFulfill() throws StateTransitionException, IOException {
        // Arrange
        MeterReading meterReading = TestResourceProvider.readMeterReadingFromFile(TestResourceProvider.CONSUMPTION_LOAD_CURVE_1_DAY);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pId", "cId", "dId",
                meterReading.start().atStartOfDay(ZONE_ID_FR),
                meterReading.end().atStartOfDay(ZONE_ID_FR),
                Granularity.P1D,
                factory);
        permissionRequest.changeState(factory.create(permissionRequest, PermissionProcessStatus.ACCEPTED).build());
        FrEnedisPermissionRequest spy = spy(permissionRequest);


        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // Act
        new IdentifiableMeterReadingService(testPublisher.flux());
        StepVerifier.create(testPublisher)
                .then(() -> testPublisher.emit(new IdentifiableMeterReading(spy, meterReading)))
                .expectNextCount(1)
                .verifyComplete();
        // Assert
        verify(spy, never()).fulfill();
    }
}
