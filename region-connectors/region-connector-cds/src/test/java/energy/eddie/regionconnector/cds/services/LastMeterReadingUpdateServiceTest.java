// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import energy.eddie.regionconnector.cds.permission.events.InternalPollingEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LastMeterReadingUpdateServiceTest {
    @Spy
    private final IdentifiableDataStreams streams = new IdentifiableDataStreams();
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private LastMeterReadingUpdateService service;
    @Captor
    private ArgumentCaptor<InternalPollingEvent> eventCaptor;

    @Test
    void testUpdateLastMeterReadings_emitsInternalPollingEvent() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .build();
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var yesterday = now.minusDays(1);
        var twoDaysAgo = now.minusDays(2);
        var segments = List.of(
                new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                        .addRelatedMeterdevicesItem("1")
                        .addRelatedMeterdevicesItem("2")
                        .segmentEnd(yesterday.toOffsetDateTime()),
                new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                        .addRelatedMeterdevicesItem("1")
                        .addRelatedMeterdevicesItem("3")
                        .segmentEnd(now.toOffsetDateTime()),
                new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                        .addRelatedMeterdevicesItem("2")
                        .segmentEnd(twoDaysAgo.toOffsetDateTime())
        );

        // When
        streams.publishValidatedHistoricalData(pr, List.of(), List.of(), List.of(), List.of(), segments);

        // Then
        StepVerifier.create(streams.validatedHistoricalData())
                    .then(streams::close)
                    .verifyComplete();
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertThat(res.lastMeterReadings())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                "1", now,
                                "2", yesterday,
                                "3", now
                        )
                );
    }
}