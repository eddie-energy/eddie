package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

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
class FluviusValidatedHistoricalDataMarketDocumentProviderTest {
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
        InputStream inputStream = classLoader.getResourceAsStream("electricity_data_measurement_daily.json");
        GetEnergyResponseModelApiDataResponse json = mapper.readValue(inputStream,
                                                                      GetEnergyResponseModelApiDataResponse.class);

        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .dataNeedId("dnid")
                                                       .granularity(Granularity.P1D)
                                                       .build();

        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT1H,
                        Granularity.P1D
                ));

        // When
        var streams = new IdentifiableDataStreams();
        streams.publish(pr, json);
        streams.close();
        var provider = new FluviusValidatedHistoricalDataMarketDocumentProvider(fluviusConfig,
                                                                                streams,
                                                                                dataNeedsService);

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .expectNextCount(1)
                    .verifyComplete();
    }
}