package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientIdException;
import energy.eddie.regionconnector.us.green.button.config.exceptions.MissingClientSecretException;
import energy.eddie.regionconnector.us.green.button.permission.events.UsCreatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMalformedEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.UsValidatedEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestCreationServiceTest {
    @SuppressWarnings("unused")
    @Spy
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "token",
            "http://localhost",
            Map.of("company", "client-id", "only-id", "client-id"),
            Map.of("company", "client-secret", "only-secret", "client-secret"),
            "http://localhost"
    );
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private PermissionRequestCreationService creationService;

    public static Stream<Arguments> createPermissionRequest_withUnsupportedDataNeed_throws() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(
                        new DataNeedCalculation(
                                false, ""
                        )
                ),
                Arguments.of(
                        new DataNeedCalculation(
                                true,
                                List.of(Granularity.PT15M),
                                null,
                                new Timeframe(now.minusDays(10), now.minusDays(1))
                        )
                ),
                Arguments.of(
                        new DataNeedCalculation(
                                true,
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                null
                        )
                ),
                Arguments.of(
                        new DataNeedCalculation(
                                true,
                                null,
                                new Timeframe(now, now),
                                new Timeframe(now.minusDays(10), now.minusDays(1))
                        )
                ),
                Arguments.of(
                        new DataNeedCalculation(
                                true,
                                List.of(),
                                new Timeframe(now, now),
                                new Timeframe(now.minusDays(10), now.minusDays(1))
                        )
                )
        );
    }

    @Test
    void findConnectionStatusMessageById_returnsConnectionStatusMessage() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new GreenButtonPermissionRequest(
                "pid",
                "cid",
                "dnid",
                now,
                now,
                Granularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                now.atStartOfDay(ZoneOffset.UTC),
                "US",
                "company",
                "http://localhost",
                "scope"
        );
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));
        // When
        var res = creationService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(res.isPresent());
        var csm = res.get();
        assertAll(
                () -> assertEquals("cid", csm.connectionId()),
                () -> assertEquals("pid", csm.permissionId()),
                () -> assertEquals("dnid", csm.dataNeedId()),
                () -> assertEquals(pr.dataSourceInformation(), csm.dataSourceInformation()),
                () -> assertEquals(pr.status(), csm.status())
        );
    }

    @Test
    void findConnectionStatusMessageById_returnsEmptyOptional_onMissingPermissionRequest() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());
        // When
        var res = creationService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    void createPermissionRequest_returnsPermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException, MissingClientIdException, MissingClientSecretException {
        // Given
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.PT1H
        );
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate(dataNeed))
                .thenReturn(
                        new DataNeedCalculation(
                                true,
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now.minusDays(10), now.minusDays(1))
                        )
                );

        // When
        var res = creationService.createPermissionRequest(new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                "company",
                "US"
        ));

        // Then
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsValidatedEvent.class));
        assertAll(
                () -> assertNotNull(res.permissionId()),
                () -> assertTrue(res.redirectUri().toString().startsWith("http://localhost"))
        );
    }

    @Test
    void createPermissionRequest_throwsOnUnknownDataNeedId() {
        // Given
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.empty());
        var permissionRequestForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                "company",
                "US"
        );

        // When
        // Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> creationService.createPermissionRequest(permissionRequestForCreation));
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsMalformedEvent.class));
    }

    @ParameterizedTest
    @MethodSource
    void createPermissionRequest_withUnsupportedDataNeed_throws(DataNeedCalculation calc) {
        // Given
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.PT1H
        );
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        when(calculationService.calculate(dataNeed))
                .thenReturn(calc);

        var permissionRequestForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                "company",
                "US"
        );

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> creationService.createPermissionRequest(permissionRequestForCreation));
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsMalformedEvent.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"only-id", "only-secret"})
    void createPermissionRequest_throwsOnInvalidConfiguration(String company) {
        // Given
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.PT1H
        );
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        var now = LocalDate.now(ZoneOffset.UTC);
        when(calculationService.calculate(dataNeed))
                .thenReturn(
                        new DataNeedCalculation(
                                true,
                                List.of(Granularity.PT15M),
                                new Timeframe(now, now),
                                new Timeframe(now.minusDays(10), now.minusDays(1))
                        )
                );
        var permissionRequestForCreation = new PermissionRequestForCreation(
                "cid",
                "dnid",
                "http://localhost",
                company,
                "US"
        );

        // When
        // Then
        assertThrows(Exception.class, () -> creationService.createPermissionRequest(permissionRequestForCreation));
        verify(outbox).commit(isA(UsCreatedEvent.class));
        verify(outbox).commit(isA(UsMalformedEvent.class));
    }
}