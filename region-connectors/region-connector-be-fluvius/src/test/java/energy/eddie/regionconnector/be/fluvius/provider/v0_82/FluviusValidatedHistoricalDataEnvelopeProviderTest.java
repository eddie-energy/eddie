package energy.eddie.regionconnector.be.fluvius.provider.v0_82;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullableModule;
import reactor.test.StepVerifier;

import java.io.InputStream;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluviusValidatedHistoricalDataEnvelopeProviderTest {

    private final ClassLoader classLoader = this.getClass().getClassLoader();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                                                          .registerModule(new JsonNullableModule());

    @Mock
    private DataNeedsService dataNeedsService;

    @Mock
    private FluviusOAuthConfiguration fluviusConfig;

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_gas_differentUnits() throws Exception {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("gas_data_measurement_hourly_different_units.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream, GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid")
                .dataNeedId("dnid")
                .granularity(Granularity.PT1H)
                .build();

        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.PT1H,
                        Granularity.PT1H
                ));

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataEnvelopeProvider(fluviusConfig, streams, dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream().collectList()
                        .map(list -> list.isEmpty() ? 0 : list.getFirst()
                                .getValidatedHistoricalDataMarketDocument()
                                .getTimeSeriesList()
                                .getTimeSeries().size()))
                .expectNext(2)
                .verifyComplete();

        // Clean-Up
        provider.close();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_gas_sameUnit() throws Exception {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("gas_data_measurement_daily_same_unit.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream, GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid")
                .dataNeedId("dnid")
                .granularity(Granularity.P1D)
                .build();

        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataEnvelopeProvider(fluviusConfig, streams, dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream().collectList()
                        .map(list -> list.isEmpty() ? 0 : list.getFirst()
                                .getValidatedHistoricalDataMarketDocument()
                                .getTimeSeriesList().getTimeSeries().getFirst().
                                getSeriesPeriodList().getSeriesPeriods().getFirst().
                                getPointList().getPoints().size()))
                .expectNext(2)
                .verifyComplete();

        // Clean-Up
        provider.close();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_gas_daily_empty() throws Exception {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("gas_data_measurement_hourly_different_units.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream, GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid")
                .dataNeedId("dnid")
                .granularity(Granularity.P1D)
                .build();

        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.NATURAL_GAS,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataEnvelopeProvider(fluviusConfig, streams, dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream().collectList()
                        .map(list -> list.isEmpty() ? 0 : list.getFirst()
                                .getValidatedHistoricalDataMarketDocument()
                                .getTimeSeriesList()
                                .getTimeSeries().size()))
                .expectNext(0)
                .verifyComplete();

        // Clean-Up
        provider.close();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_electricity_quarterHourly() throws Exception {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream, GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT15M,
                        Granularity.PT15M
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid")
                .build();

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataEnvelopeProvider(fluviusConfig, streams, dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream().collectList()
                        .map(list -> list.isEmpty() ? 0 : list.getFirst()
                                .getValidatedHistoricalDataMarketDocument()
                                .getTimeSeriesList()
                                .getTimeSeries().size()))
                .expectNext(2)
                .verifyComplete();

        // Clean-Up
        provider.close();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_electricity_hourly_empty() throws Exception {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_quarter_hourly.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream, GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid")
                .granularity(Granularity.P1D)
                .build();

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataEnvelopeProvider(fluviusConfig, streams, dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream().collectList()
                        .map(list -> list.isEmpty() ? 0 : list.getFirst()
                                .getValidatedHistoricalDataMarketDocument()
                                .getTimeSeriesList()
                                .getTimeSeries().size()))
                .expectNext(0)
                .verifyComplete();

        // Clean-Up
        provider.close();
    }

    @Test
    void testGetValidatedHistoricalDataMarketDocumentsStream_electricity_daily() throws Exception {
        // Given
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_daily.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream, GetEnergyResponseModelApiDataResponse.class);

        when(dataNeedsService.getById("did"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.P1D,
                        Granularity.P1D
                ));

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                .permissionId("pid")
                .granularity(Granularity.P1D)
                .build();

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataEnvelopeProvider(fluviusConfig, streams, dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream().collectList()
                        .map(list -> list.isEmpty() ? 0 : list.getFirst()
                                .getValidatedHistoricalDataMarketDocument()
                                .getTimeSeriesList()
                                .getTimeSeries().size()))
                .expectNext(1)
                .verifyComplete();

        // Clean-Up
        provider.close();
    }
}