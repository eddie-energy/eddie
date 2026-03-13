// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.ECMPList;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPListData;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPTimeData;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.ProcessDirectory;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.UpdateEndDateEvent;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10.ECMPList01p10;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ECMPListHandlerTest {
    @Mock
    private IdentifiableStreams streams;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<UpdateEndDateEvent> eventCaptor;

    @Test
    void givenECMPList_whenHandle_thenUpdatesEndDate() {
        // Given
        var pr = new EdaPermissionRequest(
                "cid",
                "pid",
                "dnid",
                "cm-id",
                "conv-id",
                "AT0000000000000000000000000000000",
                "AT000000",
                LocalDate.of(2026, 1, 1),
                null,
                AllowedGranularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                "message",
                "c-id",
                ZonedDateTime.now(AT_ZONE_ID)
        );
        var ecmpList = createECMPList();
        var id = new IdentifiableECMPList(new ECMPList01p10(ecmpList), pr);
        when(streams.ecmpListStream()).thenReturn(Flux.just(id));

        // When
        new ECMPListHandler(outbox, streams);

        // Then
        verify(outbox).commit(eventCaptor.capture());
        var res = eventCaptor.getValue();
        assertEquals(LocalDate.of(2027, 1, 1), res.permissionEnd());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AT2222222222222222222222222222222"})
    @NullSource
    void givenECMPListWithoutMatchingMeteringPoint_whenHandle_thenDoesNothing(String meteringPointId) {
        // Given
        var pr = new EdaPermissionRequest(
                "cid",
                "pid",
                "dnid",
                "cm-id",
                "conv-id",
                meteringPointId,
                "AT000000",
                LocalDate.of(2026, 1, 1),
                null,
                AllowedGranularity.PT15M,
                PermissionProcessStatus.ACCEPTED,
                "message",
                "c-id",
                ZonedDateTime.now(AT_ZONE_ID)
        );
        var ecmpList = createECMPList();
        var id = new IdentifiableECMPList(new ECMPList01p10(ecmpList), pr);
        when(streams.ecmpListStream()).thenReturn(Flux.just(id));

        // When
        new ECMPListHandler(outbox, streams);

        // Then
        verify(outbox, never()).commit(any());
    }

    private static ECMPList createECMPList() {
        var factory = DatatypeFactory.newDefaultInstance();
        var to1 = factory.newXMLGregorianCalendar(2026, 1, 1, 0, 0, 0, 0, 0);
        var to2 = factory.newXMLGregorianCalendar(2027, 1, 1, 0, 0, 0, 0, 0);
        return new ECMPList()
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withMessageId("ecmp-message-id")
                                .withMPListData(
                                        new MPListData()
                                                .withMeteringPoint("AT1111111111111111111111111111111"),
                                        new MPListData()
                                                .withMeteringPoint("AT0000000000000000000000000000000")
                                                .withMPTimeData(
                                                        new MPTimeData()
                                                                .withDateTo(to1),
                                                        new MPTimeData()
                                                                .withDateTo(to2)
                                                )
                                )
                );
    }
}