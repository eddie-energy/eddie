// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10.EdaECMPList01p10InboundMessageFactory;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xmlunit.builder.DiffBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class IntermediateEnergySharingReferenceDataMarketDocumentTest {

    @Test
    void testToEnvelope() throws SerdeInitializationException, SerializationException {
        // Given
        var ignoredNames = Set.of("creationDateTime");
        var serde = new XmlMessageSerde();
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("at.ebutilities.schemata");
        var factory = new EdaECMPList01p10InboundMessageFactory(marshaller);
        var ecmpList = factory.parseInputStream(getClass().getResourceAsStream("/xsd/ecmplist/_01p10/ecmplist.xml"));
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var id = new IdentifiableECMPList(ecmpList, pr);
        var intermediateDocument = new IntermediateEnergySharingReferenceDataMarketDocument(id);
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <ns:ESRDMD_Envelope xmlns:ns="https://eddie.energy/CIM/CEEDS_EnergySharingReferenceDataMarketDocument_v1.12_annotated.xsd">
                    <ns:MarketDocument>
                        <ns:mRID>AT000000000000000000000000000000000</ns:mRID>
                        <ns:revisionNumber>112</ns:revisionNumber>
                        <ns:createdDateTime>2025-10-06T07:04:58Z</ns:createdDateTime>
                        <ns:sender_MarketParticipant.name>AT000000</ns:sender_MarketParticipant.name>
                        <ns:receiver_MarketParticipant.name>CC000000</ns:receiver_MarketParticipant.name>
                        <ns:EnergyCommunity>
                            <ns:DateFrom>2025-10-05T22:00:00Z</ns:DateFrom>
                            <ns:mRID>ATCC0000DYNAMCC000000000000000000</ns:mRID>
                            <ns:AccountingPoint>
                                <ns:energySharingParticipationFactor>100</ns:energySharingParticipationFactor>
                                <ns:mRID codingScheme="NAT">AT0000000000000000000000000000000</ns:mRID>
                                <ns:energySharingEnergyDirection>A02</ns:energySharingEnergyDirection>
                            </ns:AccountingPoint>
                        </ns:EnergyCommunity>
                    </ns:MarketDocument>
                    <ns:MessageDocumentHeader>
                        <ns:creationDateTime>2026-01-01T00:00:00Z</ns:creationDateTime>
                        <ns:MetaInformation>
                            <ns:connectionId>cid</ns:connectionId>
                            <ns:requestPermissionId>pid</ns:requestPermissionId>
                            <ns:dataNeedId>dnid</ns:dataNeedId>
                            <ns:documentType>energy-sharing-reference-data-market-document</ns:documentType>
                            <ns:regionConnector>at-eda</ns:regionConnector>
                            <ns:regionCountry>AT</ns:regionCountry>
                        </ns:MetaInformation>
                    </ns:MessageDocumentHeader>
                </ns:ESRDMD_Envelope>
                """;

        // When
        var envelope = intermediateDocument.toEnvelope();

        // Then
        var xml = serde.serialize(envelope);
        XmlValidator.validateV112EnergySharingReferenceDataMarketDocument(xml);
        var myDiff = DiffBuilder.compare(expected)
                                .withTest(xml)
                                .ignoreWhitespace()
                                .ignoreComments()
                                .checkForSimilar()
                                .withNodeFilter(node -> ignoredNames.stream().noneMatch(node.getNodeName()::endsWith))
                                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription() + "\n" + new String(xml, StandardCharsets.UTF_8));
        var json = new JsonMessageSerde().serialize(envelope);
        System.out.println(new String(json));
    }
}