package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataServiceTest {
    private static final StateBuilderFactory stateBuilderFactory = new StateBuilderFactory();
    @Mock
    private PollingService pollingService;
    @Mock
    private PermissionRequestService permissionRequestService;

    @InjectMocks
    private FutureDataService futureDataService;

    static Stream<Arguments> activePermission_that_needsToBeFetched() {
        LocalDate today = LocalDate.now(ZONE_ID_FR);
        return Stream.of(
                Arguments.of(createPermissionRequest(today.minusDays(1), today, Optional.empty()),
                             "Permission: start = yesterday, end = today, no latest meter reading"),
                Arguments.of(createPermissionRequest(today.minusDays(2), today, Optional.empty()),
                             "Permission: start = 2 days ago, end = today, no latest meter reading"),
                Arguments.of(createPermissionRequest(today.minusDays(2), today, Optional.of(today.minusDays(1))),
                             "Permission: start = 2 days ago, end = today, latest meter reading = yesterday"),
                Arguments.of(createPermissionRequest(today.minusDays(2),
                                                     today.plusDays(1),
                                                     Optional.of(today.minusDays(1))),
                             "Permission: start = 2 days ago, end = tomorrow, latest meter reading = yesterday")
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static FrEnedisPermissionRequest createPermissionRequest(
            LocalDate start,
            LocalDate end,
            Optional<LocalDate> latestMeterReading
    ) {
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("cId",
                                                                                  "dId",
                                                                                  start,
                                                                                  end,
                                                                                  Granularity.P1D,
                                                                                  stateBuilderFactory);
        latestMeterReading.ifPresent(permissionRequest::updateLatestMeterReading);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, PermissionProcessStatus.ACCEPTED)
                                                         .build());
        return permissionRequest;
    }

    static Stream<Arguments> permissionThatAreInactiveOrDoNotNeedToBeFetched() {
        LocalDate today = LocalDate.now(ZONE_ID_FR);
        return Stream.of(
                Arguments.of(createPermissionRequest(today.minusDays(1), today.plusDays(1), Optional.of(today)),
                             "Permission: start = yesterday, end = tomorrow, latest meter reading = today, data is already up to date"),
                Arguments.of(createPermissionRequest(today, today, Optional.empty()),
                             "Permission: start = today, end = today, inactive"),
                Arguments.of(createPermissionRequest(today.plusDays(1), today.plusDays(2), Optional.empty()),
                             "Permission: start = tomorrow, end = day after tomorrow, inactive"),
                Arguments.of(createPermissionRequest(today.plusDays(2), today.plusDays(2), Optional.empty()),
                             "Permission: start = day after tomorrow, end = day after tomorrow, inactive")
        );
    }

    @Test
    void fetchMeterReadings_doesNothingIfNoAcceptedPermissionRequestsAreFound() {
        // Given
        when(permissionRequestService.findAllAcceptedPermissionRequests()).thenReturn(Collections.emptyList());

        // When
        futureDataService.fetchMeterReadings();

        // Then
        verify(permissionRequestService, times(1)).findAllAcceptedPermissionRequests();
        verifyNoMoreInteractions(pollingService); // No interaction with pollingService should occur
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("activePermission_that_needsToBeFetched")
    void fetchMeterReadings_invokesPollingService_forActivePermissionsThatNeedToBePolled(
            FrEnedisPermissionRequest permissionRequest,
            String description
    ) {
        // Given
        when(permissionRequestService.findAllAcceptedPermissionRequests()).thenReturn(Collections.singletonList(
                permissionRequest));

        // When
        futureDataService.fetchMeterReadings();

        // Then
        verify(permissionRequestService, times(1)).findAllAcceptedPermissionRequests();
        LocalDate startFetchDate = permissionRequest.latestMeterReading()
                                                    .orElse(permissionRequest.start().toLocalDate());
        verify(pollingService, times(1)).fetchMeterReadings(permissionRequest,
                                                            startFetchDate,
                                                            LocalDate.now(ZONE_ID_FR));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("permissionThatAreInactiveOrDoNotNeedToBeFetched")
    void fetchMeterReadings_doesNotInvokePollingService_IfPermissionIsActiveButDoesNotNeedToBeFetched(
            FrEnedisPermissionRequest permissionRequest,
            String description
    ) {
        // Given
        when(permissionRequestService.findAllAcceptedPermissionRequests()).thenReturn(Collections.singletonList(
                permissionRequest));

        // Execute
        futureDataService.fetchMeterReadings();

        // Then
        verify(permissionRequestService, times(1)).findAllAcceptedPermissionRequests();
        verifyNoMoreInteractions(pollingService); // No interaction with pollingService should occur
    }
}
