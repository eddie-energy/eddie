package energy.eddie.core.services;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.pmd.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminationRouterTest {
    @Mock
    private RegionConnector regionConnector1;
    @Mock
    private RegionConnector regionConnector2;
    @Mock
    private RegionConnectorMetadata metadata1;
    @Mock
    private RegionConnectorMetadata metadata2;
    @Mock
    private TerminationConnector connector;

    @Test
    void testTerminationMessage_withRegionConnectorId() {
        // Given
        TestPublisher<Pair<String, PermissionEnvelope>> publisher = TestPublisher.create();
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter();
        router.registerTerminationConnector(connector);

        when(metadata1.id()).thenReturn("id");
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        when(metadata2.id()).thenReturn("other-id");
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("pid")
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(
                                        new PermissionMarketDocumentComplexType.PermissionList()
                                                .withPermissions(new PermissionComplexType()
                                                                         .withReasonList(
                                                                                 new PermissionComplexType.ReasonList()
                                                                                         .withReasons(
                                                                                                 new ReasonComplexType()
                                                                                                         .withCode(
                                                                                                                 ReasonCodeTypeList.CANCELLED_EP
                                                                                                         )
                                                                                         )
                                                                         )
                                                )
                                )
                );

        var pair = new Pair<>("id", pmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, times(1)).terminatePermission("pid");
        verify(regionConnector2, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withMktActivityRecord() {
        // Given
        TestPublisher<Pair<String, PermissionEnvelope>> publisher = TestPublisher.create();
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter();
        router.registerTerminationConnector(connector);

        when(metadata1.id()).thenReturn("id");
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        when(metadata2.id()).thenReturn("other-id");
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("pid")
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withReasonList(new PermissionComplexType.ReasonList()
                                                                                                             .withReasons(
                                                                                                                     new ReasonComplexType()
                                                                                                                             .withCode(
                                                                                                                                     ReasonCodeTypeList.CANCELLED_EP)
                                                                                                             )
                                                                                     )
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withType(
                                                                                                                                                "id"))
                                                                                     )
                                                            )
                                )
                );
        var pair = new Pair<String, PermissionEnvelope>(null, pmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, times(1)).terminatePermission("pid");
        verify(regionConnector2, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withoutMatchingRegionConnectorIdOrMktActivityRecord() {
        // Given
        TestPublisher<Pair<String, PermissionEnvelope>> publisher = TestPublisher.create();
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter();
        router.registerTerminationConnector(connector);

        when(metadata1.id()).thenReturn("id");
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        when(metadata2.id()).thenReturn("other-id");
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("pid")
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withReasonList(new PermissionComplexType.ReasonList()
                                                                                                             .withReasons(
                                                                                                                     new ReasonComplexType()
                                                                                                                             .withCode(
                                                                                                                                     ReasonCodeTypeList.CANCELLED_EP)
                                                                                                             )
                                                                                     )
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withType(
                                                                                                                                                "unknown-id"))
                                                                                     )
                                                            )
                                )
                );
        var pair = new Pair<String, PermissionEnvelope>(null, pmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, never()).terminatePermission(anyString());
        verify(regionConnector2, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withMissingTypeAttribute() {
        // Given
        TestPublisher<Pair<String, PermissionEnvelope>> publisher = TestPublisher.create();
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter();
        router.registerTerminationConnector(connector);

        when(metadata1.id()).thenReturn("id");
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        router.registerRegionConnector(regionConnector1);
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("pid")
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withReasonList(new PermissionComplexType.ReasonList()
                                                                                                             .withReasons(
                                                                                                                     new ReasonComplexType()
                                                                                                                             .withCode(
                                                                                                                                     ReasonCodeTypeList.CANCELLED_EP)
                                                                                                             )
                                                                                     )
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withType(
                                                                                                                                                "id"))
                                                                                     )
                                                            )
                                )
                );
        var pair = new Pair<String, PermissionEnvelope>(null, pmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withMissingReason() {
        // Given
        TestPublisher<Pair<String, PermissionEnvelope>> publisher = TestPublisher.create();
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter();
        router.registerTerminationConnector(connector);

        when(metadata1.id()).thenReturn("id");
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        router.registerRegionConnector(regionConnector1);
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("pid")
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withReasonList(new PermissionComplexType.ReasonList()
                                                                                                             .withReasons()
                                                                                     )
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withType(
                                                                                                                                                "id"))
                                                                                     )
                                                            )
                                )
                );
        var pair = new Pair<String, PermissionEnvelope>(null, pmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withoutMatchingRegionConnector() {
        // Given
        TestPublisher<Pair<String, PermissionEnvelope>> publisher = TestPublisher.create();
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter();
        router.registerTerminationConnector(connector);

        when(metadata1.id()).thenReturn("id");
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        when(metadata2.id()).thenReturn("other-id");
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("pid")
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withSenderMarketParticipantMRID(new PartyIDStringComplexType()
                                                                         .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                                )
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withReasonList(new PermissionComplexType.ReasonList()
                                                                                                             .withReasons(
                                                                                                                     new ReasonComplexType()
                                                                                                                             .withCode(
                                                                                                                                     ReasonCodeTypeList.CANCELLED_EP
                                                                                                                             )
                                                                                                             )
                                                                                     )
                                                            )
                                )
                );
        var pair = new Pair<String, PermissionEnvelope>(null, pmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, never()).terminatePermission(anyString());
        verify(regionConnector2, never()).terminatePermission(anyString());
    }
}