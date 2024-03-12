package energy.eddie.core.services;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.cmd.*;
import org.junit.jupiter.api.Test;
import reactor.test.publisher.TestPublisher;

import java.util.Optional;

import static org.mockito.Mockito.*;

class TerminationRouterTest {

    @Test
    void testTerminationMessage_withRegionConnectorId() {
        // Given
        TestPublisher<Pair<String, ConsentMarketDocument>> publisher = TestPublisher.create();
        var connector = mock(TerminationConnector.class);
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter(Optional.of(connector));

        var metadata1 = mock(RegionConnectorMetadata.class);
        when(metadata1.id()).thenReturn("id");
        var regionConnector1 = mock(RegionConnector.class);
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        var metadata2 = mock(RegionConnectorMetadata.class);
        when(metadata2.id()).thenReturn("other-id");
        var regionConnector2 = mock(RegionConnector.class);
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID("pid")
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withPermissionList(new ConsentMarketDocument.PermissionList()
                        .withPermissions(new PermissionComplexType()
                                .withReasonList(new PermissionComplexType.ReasonList()
                                        .withReasons(new ReasonComplexType()
                                                .withCode(ReasonCodeTypeList.CANCELLED_EP)
                                        )
                                )
                        )
                );
        var pair = new Pair<>("id", cmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, times(1)).terminatePermission("pid");
        verify(regionConnector2, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withMktActivityRecord() {
        // Given
        TestPublisher<Pair<String, ConsentMarketDocument>> publisher = TestPublisher.create();
        var connector = mock(TerminationConnector.class);
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter(Optional.of(connector));

        var metadata1 = mock(RegionConnectorMetadata.class);
        when(metadata1.id()).thenReturn("id");
        when(metadata1.countryCode()).thenReturn("AT");
        var regionConnector1 = mock(RegionConnector.class);
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        var metadata2 = mock(RegionConnectorMetadata.class);
        when(metadata2.id()).thenReturn("other-id");
        when(metadata2.countryCode()).thenReturn("DK");
        var regionConnector2 = mock(RegionConnector.class);
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID("pid")
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withPermissionList(new ConsentMarketDocument.PermissionList()
                        .withPermissions(new PermissionComplexType()
                                .withReasonList(new PermissionComplexType.ReasonList()
                                        .withReasons(new ReasonComplexType()
                                                .withCode(ReasonCodeTypeList.CANCELLED_EP)
                                        )
                                ).withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                        .withMktActivityRecords(new MktActivityRecordComplexType()
                                                .withType("id"))
                                )
                        )
                );
        var pair = new Pair<String, ConsentMarketDocument>(null, cmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, times(1)).terminatePermission("pid");
        verify(regionConnector2, never()).terminatePermission(anyString());
    }

    @Test
    void testTerminationMessage_withoutMatchingRegionConnectorIdOrMktActivityRecord() {
        // Given
        TestPublisher<Pair<String, ConsentMarketDocument>> publisher = TestPublisher.create();
        var connector = mock(TerminationConnector.class);
        when(connector.getTerminationMessages()).thenReturn(publisher.flux());
        var router = new TerminationRouter(Optional.of(connector));

        var metadata1 = mock(RegionConnectorMetadata.class);
        when(metadata1.id()).thenReturn("id");
        when(metadata1.countryCode()).thenReturn("FR");
        var regionConnector1 = mock(RegionConnector.class);
        when(regionConnector1.getMetadata()).thenReturn(metadata1);

        var metadata2 = mock(RegionConnectorMetadata.class);
        when(metadata2.id()).thenReturn("other-id");
        when(metadata2.countryCode()).thenReturn("DK");
        var regionConnector2 = mock(RegionConnector.class);
        when(regionConnector2.getMetadata()).thenReturn(metadata2);

        router.registerRegionConnector(regionConnector1);
        router.registerRegionConnector(regionConnector2);
        ConsentMarketDocument cmd = new ConsentMarketDocument()
                .withMRID("pid")
                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                .withSenderMarketParticipantMRID(new PartyIDStringComplexType()
                        .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                )
                .withPermissionList(new ConsentMarketDocument.PermissionList()
                        .withPermissions(new PermissionComplexType()
                                .withReasonList(new PermissionComplexType.ReasonList()
                                        .withReasons(new ReasonComplexType()
                                                .withCode(ReasonCodeTypeList.CANCELLED_EP)
                                        )
                                )
                        )
                );
        var pair = new Pair<String, ConsentMarketDocument>(null, cmd);

        // When
        publisher.emit(pair);

        // Then
        verify(regionConnector1, never()).terminatePermission(anyString());
        verify(regionConnector2, never()).terminatePermission(anyString());
    }
}