// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers;

import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdentifiableDataStreamsTest {

    @SuppressWarnings("resource")
    @Test
    void testPublishValidatedHistoricalData_publishes() {
        // Given
        var streams = new IdentifiableDataStreams();
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        streams.publishValidatedHistoricalData(pr, List.of(), List.of(), List.of(), List.of(), List.of());

        // Then
        StepVerifier.create(streams.validatedHistoricalData())
                    .then(streams::close)
                    .assertNext(res -> assertAll(
                            () -> assertEquals(List.of(), res.payload().usageSegments()),
                            () -> assertSame(pr, res.permissionRequest())
                    ))
                    .verifyComplete();
    }

    @Test
    @SuppressWarnings("resource")
    void testPublishAccountingPointData_publishes() {
        // Given
        var streams = new IdentifiableDataStreams();
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();

        // When
        StepVerifier.create(streams.accountingPointData())
                    .then(() ->
                                  streams.publishAccountingPointData(pr,
                                                                     List.of(),
                                                                     List.of(),
                                                                     List.of(),
                                                                     List.of()
                                  )
                    )
                    .then(streams::close)
                    // Then
                    .assertNext(res -> assertSame(pr, res.permissionRequest()))
                    .verifyComplete();
    }
}