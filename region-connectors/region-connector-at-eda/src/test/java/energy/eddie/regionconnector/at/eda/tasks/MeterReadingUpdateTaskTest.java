// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.SimpleEdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.events.DataReceivedEvent;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingUpdateTaskTest {
    @Mock
    private Outbox outbox;
    @Mock
    private IdentifiableStreams streams;
    @Captor
    private ArgumentCaptor<DataReceivedEvent> eventCaptor;

    @Test
    void testUpdateMeterReading_updatesMeterReading_forEachPermissionRequest() {
        // Given
        var today = LocalDate.now(ZoneOffset.UTC);
        TestPublisher<IdentifiableConsumptionRecord> publisher = TestPublisher.create();
        when(streams.consumptionRecordStream()).thenReturn(publisher.flux());
        new MeterReadingUpdateTask(outbox, streams);

        // When
        publisher.emit(new IdentifiableConsumptionRecord(
                new SimpleEdaConsumptionRecord(),
                List.of(new SimplePermissionRequest("pid",
                                                    "cid",
                                                    "dnid",
                                                    "cmRequestId",
                                                    "cid",
                                                    PermissionProcessStatus.VALIDATED)),
                today,
                today
        ));

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals(PermissionProcessStatus.VALIDATED, res.status()),
                () -> assertEquals(today, res.start()),
                () -> assertEquals(today, res.end())
        );
    }
}