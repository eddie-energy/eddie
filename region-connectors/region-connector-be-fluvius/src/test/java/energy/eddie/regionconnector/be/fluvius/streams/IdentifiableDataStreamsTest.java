// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.streams;

import energy.eddie.regionconnector.be.fluvius.client.model.v3.ApiMetaData;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.EnergyType;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModel;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.Headpoint;
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
                new GetEnergyResponseModel(new Headpoint(null, EnergyType.ELECTRICITY, null))
        ));

        // Then
        StepVerifier.create(streams.getMeteringData())
                    .expectNextCount(1)
                    .then(streams::close)
                    .verifyComplete();
    }
}