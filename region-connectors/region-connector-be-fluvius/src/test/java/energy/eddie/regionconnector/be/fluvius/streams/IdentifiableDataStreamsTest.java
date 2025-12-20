package energy.eddie.regionconnector.be.fluvius.streams;

import energy.eddie.regionconnector.be.fluvius.client.model.ApiMetaData;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class IdentifiableDataStreamsTest {

    @SuppressWarnings("resource")
    @Test
    void testPublish_publishesResults() {
        // Given
        var streams = new IdentifiableDataStreams();
        var pr = DefaultFluviusPermissionRequestBuilder.create()
                                                       .permissionId("pid")
                                                       .build();

        // When
        streams.publish(pr, new GetEnergyResponseModelApiDataResponse(
                new ApiMetaData(null),
                new GetEnergyResponseModel(null, null, null)
        ));

        // Then
        StepVerifier.create(streams.getMeteringData())
                    .expectNextCount(1)
                    .then(streams::close)
                    .verifyComplete();
    }
}