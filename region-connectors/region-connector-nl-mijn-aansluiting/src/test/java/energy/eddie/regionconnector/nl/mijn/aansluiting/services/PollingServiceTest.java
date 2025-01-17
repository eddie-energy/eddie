package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.fasterxml.jackson.core.type.TypeReference;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.ApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.MijnAansluitingApi;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.ConsumptionData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.AccessTokenAndSingleSyncUrl;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlInternalPollingEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.oauth.NoRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.api.v0.PermissionProcessStatus.FULFILLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {
    private final JsonResourceObjectMapper<List<MijnAansluitingResponse>> vhdMapper = new JsonResourceObjectMapper<>(new TypeReference<>() {});
    private final JsonResourceObjectMapper<List<ConsumptionData>> apMapper = new JsonResourceObjectMapper<>(new TypeReference<>() {});
    @Mock
    private OAuthManager oAuthManager;
    @Mock
    private Outbox outbox;
    @Mock
    private ApiClient apiClient;
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed dataNeed;
    @InjectMocks
    private PollingService pollingService;
    @Captor
    private ArgumentCaptor<NlSimpleEvent> captor;
    @Captor
    private ArgumentCaptor<NlInternalPollingEvent> internalPollingEventCaptor;

    public static Stream<Arguments> testFetchConsumptionData_wrongPermissionRequest() {
        return Stream.of(
                Arguments.of(OAuthTokenDetailsNotFoundException.class),
                Arguments.of(JWTSignatureCreationException.class),
                Arguments.of(OAuthUnavailableException.class)
        );
    }

    public static Stream<Arguments> testFetchConsumptionData_revokedPermissionRequest() {
        return Stream.of(
                Arguments.of(IllegalTokenException.class),
                Arguments.of(OAuthException.class),
                Arguments.of(NoRefreshTokenException.class)
        );
    }

    public static Stream<Arguments> testFetchConsumptionData_publishesOnlyNeededEnergyType() {
        return Stream.of(
                Arguments.of(EnergyType.ELECTRICITY, 2),
                Arguments.of(EnergyType.NATURAL_GAS, 1)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFetchConsumptionData_wrongPermissionRequest(Class<Exception> exceptionClass) throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException {
        // Given
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenThrow(exceptionClass);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                null,
                null,
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        verify(outbox, never()).commit(any());
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .expectComplete()
                    .verify();
    }

    @ParameterizedTest
    @MethodSource
    void testFetchConsumptionData_revokedPermissionRequest(Class<Exception> exceptionClass) throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException {
        // Given
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenThrow(exceptionClass);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                null,
                null,
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.REVOKED, captor.getValue().status());
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .expectComplete()
                    .verify();
    }

    @Test
    void testFetchConsumptionData_publishesInternalPollingEvent() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        when(dataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        verify(outbox).commit(internalPollingEventCaptor.capture());
        var res = internalPollingEventCaptor.getValue();
        var end = ZonedDateTime.parse("2023-06-30T00:00:00.000+02:00").withZoneSameInstant(ZoneOffset.UTC);
        assertThat(res.lastMeterReadings()).containsOnlyKeys("E0003000007083514", "E0003000007083514")
                                           .containsEntry("E0003000007083514", end)
                                           .containsEntry("E0003000007083514", end);
    }

    @Test
    void testFetchConsumptionData_doesNotPublish_onEmptyResponse() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException {
        // Given
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(List.of()));
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        verify(outbox, never()).commit(any());
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource
    void testFetchConsumptionData_publishesOnlyNeededEnergyType(
            EnergyType energyType,
            int registerSize
    ) throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        when(dataNeed.energyType())
                .thenReturn(energyType);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 15),
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .assertNext(imd -> assertAll(
                            () -> assertEquals(pr, imd.permissionRequest()),
                            () -> assertEquals(1, imd.meteredData().size()),
                            () -> assertEquals(registerSize,
                                               imd.meteredData()
                                                  .getFirst()
                                                  .getMarketEvaluationPoint()
                                                  .getRegisterList()
                                                  .size())
                    ))
                    .verifyComplete();
    }

    @Test
    void testFetchConsumptionData_doesEmitNothing_onUnknownDataNeed() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.empty());
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        verify(outbox, never()).commit(any());
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .verifyComplete();
    }

    @Test
    void testFetchConsumptionData_doesEmitNothing_onInvalidDataNeed() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(mock(AccountingPointDataNeed.class)));
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        verify(outbox, never()).commit(any());
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .verifyComplete();
    }

    @Test
    void testFetchConsumptionData_doesNotPublishDataBeforeStartDate() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        when(dataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);
        var start = LocalDate.of(2023, 5, 15);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .assertNext(imd -> assertAll(
                            () -> assertEquals(start,
                                               imd.meteredData()
                                                  .getFirst()
                                                  .getMarketEvaluationPoint()
                                                  .getRegisterList()
                                                  .getFirst()
                                                  .getReadingList()
                                                  .getFirst()
                                                  .getDateAndOrTime()
                                                  .getDateTime()
                                                  .toLocalDate())
                    ))
                    .verifyComplete();
    }

    @Test
    void testFetchConsumptionData_doesNotPublishDataAfterEndDate() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        when(dataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);
        var start = LocalDate.of(2023, 5, 15);
        var end = LocalDate.of(2023, 5, 31);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                end,
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .assertNext(imd -> assertAll(
                            () -> assertEquals(end,
                                               imd.meteredData()
                                                  .getFirst()
                                                  .getMarketEvaluationPoint()
                                                  .getRegisterList()
                                                  .getFirst()
                                                  .getReadingList()
                                                  .getLast()
                                                  .getDateAndOrTime()
                                                  .getDateTime()
                                                  .toLocalDate())
                    ))
                    .verifyComplete();
    }

    @Test
    void testFetchConsumptionData_calculatesDeltas() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = vhdMapper.loadTestJson("single_consumption_data.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.CONTINUOUS_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchConsumptionData("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        when(dataNeedsService.findById("dnid"))
                .thenReturn(Optional.of(dataNeed));
        when(dataNeed.energyType())
                .thenReturn(EnergyType.ELECTRICITY);
        var start = LocalDate.of(2023, 5, 1);
        var end = LocalDate.of(2023, 5, 3);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                start,
                end,
                Granularity.P1D
        );

        // When
        pollingService.fetchConsumptionData(pr);

        // Then
        StepVerifier.create(pollingService.identifiableMeteredDataFlux())
                    .then(pollingService::close)
                    .assertNext(imd -> {
                        var readingList = imd.meteredData()
                                             .getFirst()
                                             .getMarketEvaluationPoint()
                                             .getRegisterList()
                                             .getFirst()
                                             .getReadingList();
                        assertAll(
                                () -> assertEquals(2, readingList.size()),
                                () -> assertEquals(100.0, readingList.getFirst().getValue().doubleValue()),
                                () -> assertEquals(110.0, readingList.get(1).getValue().doubleValue())
                        );
                    })
                    .verifyComplete();
    }

    @Test
    void testFetchAccountingPointData_emitsData() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException, IOException {
        // Given
        var json = apMapper.loadTestJson("single_request.json");
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.SINGLE_CONSENT_API))
                .thenReturn(new AccessTokenAndSingleSyncUrl("accessToken", "singleSync"));
        when(apiClient.fetchSingleReading("singleSync", "accessToken"))
                .thenReturn(Mono.just(json));
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchAccountingPointData(pr);

        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(FULFILLED, event.status())));
        StepVerifier.create(pollingService.identifiableAccountingPointDataFlux())
                    .then(pollingService::close)
                    .assertNext(iap -> assertAll(
                            () -> assertEquals(pr, iap.permissionRequest()),
                            () -> assertEquals(json, iap.payload())
                    ))
                    .verifyComplete();
    }

    @Test
    void testFetchAccountingPointData_withInvalidToken_doesNotFetch() throws JWTSignatureCreationException, OAuthUnavailableException, OAuthException, NoRefreshTokenException, IllegalTokenException {
        // Given
        when(oAuthManager.accessTokenAndSingleSyncUrl("pid", MijnAansluitingApi.SINGLE_CONSENT_API))
                .thenThrow(OAuthException.class);
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                "",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.of(2023, 5, 1),
                LocalDate.of(2023, 5, 31),
                Granularity.P1D
        );

        // When
        pollingService.fetchAccountingPointData(pr);

        // Then
        verify(apiClient, never()).fetchSingleReading(any(), any());
    }
}