// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinMaxEnvelopeInboundProcessingStrategyTest {
    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final String METER_ID = "003114735";

    @Mock
    private ConnectionLimitPersistenceService persistenceService;
    @Mock
    private InboundRecord inboundRecord;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private Permission permission;
    @Captor
    private ArgumentCaptor<List<ConnectionLimit>> limitsCaptor;

    private MinMaxEnvelopeInboundProcessingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MinMaxEnvelopeInboundProcessingStrategy(ObjectMapperCreatorUtil.mapper(), persistenceService);

        lenient().when(inboundRecord.dataSource()).thenReturn(inboundDataSource);
        lenient().when(inboundDataSource.permission()).thenReturn(permission);
        lenient().when(permission.id()).thenReturn(PERMISSION_ID);
        lenient().when(permission.meterId()).thenReturn(METER_ID);
    }

    @Test
    void givenNewDocument_upsertsAndMapsAccepted() {
        when(inboundRecord.payload()).thenReturn(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0"));
        when(persistenceService.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(persistenceService.upsertByCreatedAt(any())).thenReturn(2);

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.accepted(), result);
        verify(persistenceService).upsertByCreatedAt(any());
        verify(persistenceService, never()).replaceByMrid(anyString(), any());
    }

    @Test
    void givenPartiallyPersistedDocument_mapsPartiallyAccepted() {
        when(inboundRecord.payload()).thenReturn(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0"));
        when(persistenceService.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(persistenceService.upsertByCreatedAt(any())).thenReturn(1);

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.PARTIALLY_ACCEPTED, result.status());
        assertEquals("1 of 2 connection limit points were ignored because newer or equally recent values already exist",
                     result.reason());
    }

    @Test
    void givenFullyIgnoredDocument_mapsRejected() {
        when(inboundRecord.payload()).thenReturn(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0"));
        when(persistenceService.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(persistenceService.upsertByCreatedAt(any())).thenReturn(0);

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals("2 of 2 connection limit points were ignored because newer or equally recent values already exist",
                     result.reason());
    }

    @Test
    void givenSameMridAndHigherRevision_replacesAndMapsAccepted() {
        when(inboundRecord.payload()).thenReturn(payload("document-1", "2", "4.0", "9.0", "5.0", "10.0"));
        when(persistenceService.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.of(1));

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.accepted(), result);
        verify(persistenceService).replaceByMrid(eq("document-1"), any());
        verify(persistenceService, never()).upsertByCreatedAt(any());
    }

    @Test
    void givenSameMridAndLowerOrSameRevision_rejectsDocument() {
        when(inboundRecord.payload()).thenReturn(payload("document-1", "2", "4.0", "9.0", "5.0", "10.0"));
        when(persistenceService.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.of(2));

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals("Rejected connection limit document for permission %s and mRID document-1: ".formatted(
                PERMISSION_ID) + "Revision 2 is not newer than the latest processed revision 2", result.reason());
        verify(persistenceService, never()).replaceByMrid(anyString(), any());
        verify(persistenceService, never()).upsertByCreatedAt(any());
    }

    @Test
    void givenMismatchingPermissionId_rejectsWithoutPersisting() {
        var documentPermissionId = UUID.fromString("11213495-bdbf-4497-8695-5d811e45aa64");
        when(inboundRecord.payload()).thenReturn(payload("document-1", "1", "2.0", "8.0", "3.0", "7.0").replace(
                PERMISSION_ID.toString(),
                documentPermissionId.toString()));

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals("Rejected connection limit document for permission %s and mRID document-1: ".formatted(
                PERMISSION_ID) + "Document permission id %s does not match receiving permission id %s".formatted(
                documentPermissionId,
                PERMISSION_ID), result.reason());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void givenMismatchingMeterId_rejectsWithoutPersisting() {
        when(inboundRecord.payload()).thenReturn(payload("different-meter",
                                                         "document-1",
                                                         "1",
                                                         "2.0",
                                                         "8.0",
                                                         "3.0",
                                                         "7.0",
                                                         "2026-02-16T10:11:58Z"));

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals("Rejected connection limit document for permission %s and mRID document-1: ".formatted(
                PERMISSION_ID) + "Document meter id %s does not match permission meter id %s".formatted(
                "different-meter",
                METER_ID), result.reason());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void givenMissingAssetMeterId_fallsBackToPermissionMeterId() {
        when(inboundRecord.payload()).thenReturn(payload("",
                                                         "document-1",
                                                         "1",
                                                         "2.0",
                                                         "8.0",
                                                         "3.0",
                                                         "7.0",
                                                         "2026-02-16T10:11:58Z"));
        when(persistenceService.findMaxRevisionNumberByMrid("document-1")).thenReturn(Optional.empty());
        when(persistenceService.upsertByCreatedAt(any())).thenReturn(2);

        strategy.process(inboundRecord);

        verify(persistenceService).upsertByCreatedAt(limitsCaptor.capture());
        assertEquals(METER_ID, limitsCaptor.getValue().getFirst().meterId());
    }

    @Test
    void givenDocumentWithoutPoints_rejectsWithoutPersisting() {
        when(inboundRecord.payload()).thenReturn(payloadWithoutPoints());

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals("Rejected connection limit document for permission %s and mRID document-1: ".formatted(
                PERMISSION_ID) + "The document does not contain any connection limit points", result.reason());
        verifyNoInteractions(persistenceService);
    }

    @ParameterizedTest
    @MethodSource
    void givenInvalidFields_rejectsWithoutPersisting(String payload, String expectedReason) {
        when(inboundRecord.payload()).thenReturn(payload);

        var result = strategy.process(inboundRecord);

        assertEquals(InboundProcessingResult.Status.REJECTED, result.status());
        assertEquals(expectedReason, result.reason());
        verifyNoInteractions(persistenceService);
    }

    private static Stream<Arguments> givenInvalidFields_rejectsWithoutPersisting() {
        return Stream.of(argumentSet("Empty mRID",
                                     payload("", "1", "2.0", "8.0", "3.0", "7.0"),
                                     "Rejected connection limit document for permission %s: MRID is missing".formatted(
                                             PERMISSION_ID)),
                         argumentSet("No integer revision number",
                                     payload("document-1", "abc", "2.0", "8.0", "3.0", "7.0"),
                                     "Rejected connection limit document for permission %s and mRID document-1: Invalid revisionNumber abc".formatted(
                                             PERMISSION_ID)),
                         argumentSet("Invalid revision number",
                                     payload("document-1", "0", "2.0", "8.0", "3.0", "7.0"),
                                     "Rejected connection limit document for permission %s and mRID document-1: revisionNumber must be >= 1, was 0".formatted(
                                             PERMISSION_ID)));
    }

    private static String payload(String mrid, String revision, String min1, String max1, String min2, String max2) {
        return payload(METER_ID, mrid, revision, min1, max1, min2, max2, "2026-02-16T10:11:58Z");
    }

    private static String payload(
            String meterId,
            String mrid,
            String revision,
            String min1,
            String max1,
            String min2,
            String max2,
            String creationDateTime
    ) {
        var intervalStart = "2026-06-01T00:00:00Z";
        var intervalEnd = "2026-06-01T01:00:00Z";
        return """
                {
                  "MessageDocumentHeader": {
                    "creationDateTime": "%s",
                    "MetaInformation": {
                      "requestPermissionId": "00213495-bdbf-4497-8695-5d811e45aa64",
                      "Asset": { "meterId": "%s" }
                    }
                  },
                  "MarketDocument": {
                    "mRID": "%s",
                    "revisionNumber": "%s",
                    "sender_MarketParticipant.marketRole.type": "A56",
                    "receiver_MarketParticipant.marketRole.type": "A13",
                    "TimeSeries_Series": [
                      {
                        "Series": [
                          {
                            "Period": [
                              {
                                "resolution": "PT15M",
                                "timeInterval": { "start": "%s", "end": "%s" },
                                "Point": [
                                  { "position": 1, "min_Quantity.quantity": %s, "max_Quantity.quantity": %s },
                                  { "position": 2, "min_Quantity.quantity": %s, "max_Quantity.quantity": %s }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(creationDateTime,
                              meterId,
                              mrid,
                              revision,
                              intervalStart,
                              intervalEnd,
                              min1,
                              max1,
                              min2,
                              max2);
    }

    private static String payloadWithoutPoints() {
        return """
                {
                  "MessageDocumentHeader": {
                    "creationDateTime": "2026-02-16T10:11:58Z",
                    "MetaInformation": {
                      "requestPermissionId": "00213495-bdbf-4497-8695-5d811e45aa64",
                      "Asset": { "meterId": "003114735" }
                    }
                  },
                  "MarketDocument": {
                    "mRID": "document-1",
                    "revisionNumber": "1",
                    "sender_MarketParticipant.marketRole.type": "A56",
                    "receiver_MarketParticipant.marketRole.type": "A13",
                    "TimeSeries_Series": []
                  }
                }
                """;
    }
}
