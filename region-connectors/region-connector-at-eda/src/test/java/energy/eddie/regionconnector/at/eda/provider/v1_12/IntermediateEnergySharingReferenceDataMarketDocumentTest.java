// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.v1_12;

import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p20.EdaECMPList01p20InboundMessageFactory;
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
        var factory = new EdaECMPList01p20InboundMessageFactory(marshaller);
        var ecmpList = factory.parseInputStream(getClass().getResourceAsStream("/xsd/ecmplist/_01p20/ecmplist.xml"));
        var pr = new SimplePermissionRequest("pid", "cid", "dnid");
        var id = new IdentifiableECMPList(ecmpList, pr);
        var intermediateDocument = new IntermediateEnergySharingReferenceDataMarketDocument(id);
        // language=XML
        var expected = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <ns:ESRDMD_Envelope xmlns:ns="https://insieme.energy/EnergySharingReferenceDataMarketDocument_annotated_v1_12">
                    <ns:MarketDocument>
                        <ns:mRID>123456789</ns:mRID>
                        <ns:revisionNumber>112</ns:revisionNumber>
                        <ns:createdDateTime>2022-12-17T09:30:47Z</ns:createdDateTime>
                        <ns:sender_MarketParticipant.name>AT001000</ns:sender_MarketParticipant.name>
                        <ns:receiver_MarketParticipant.name>RC100123</ns:receiver_MarketParticipant.name>
                        <ns:process.processType>A55</ns:process.processType>
                        <ns:EnergyCommunity>
                            <ns:mRID>AT00100000000RC100123000000123456</ns:mRID>
                            <ns:dateFrom>2022-10-31Z</ns:dateFrom>
                            <ns:AccountingPoint>
                                <ns:mRID codingScheme="NAT">AT0010000103600000000123456123456</ns:mRID>
                                <ns:energySharingParticipationFactor>100</ns:energySharingParticipationFactor>
                                <ns:energySharingEnergyDirection>A01</ns:energySharingEnergyDirection>
                            </ns:AccountingPoint>
                        </ns:EnergyCommunity>
                        <ns:EnergyCommunity>
                            <ns:mRID>AT00100000000RC100123000000123456</ns:mRID>
                            <ns:dateFrom>2022-10-31Z</ns:dateFrom>
                            <ns:AccountingPoint>
                                <ns:mRID codingScheme="NAT">AT0010000103600000000123456123457</ns:mRID>
                                <ns:energySharingParticipationFactor>35</ns:energySharingParticipationFactor>
                                <ns:energySharingEnergyDirection>A02</ns:energySharingEnergyDirection>
                            </ns:AccountingPoint>
                        </ns:EnergyCommunity>
                        <ns:EnergyCommunity>
                            <ns:mRID>AT00100000000RC100123000000123456</ns:mRID>
                            <ns:dateFrom>2022-11-30Z</ns:dateFrom>
                            <ns:AccountingPoint>
                                <ns:mRID codingScheme="NAT">AT0010000103600000000123456123458</ns:mRID>
                                <ns:energySharingParticipationFactor>88</ns:energySharingParticipationFactor>
                                <ns:energySharingEnergyDirection>A02</ns:energySharingEnergyDirection>
                            </ns:AccountingPoint>
                        </ns:EnergyCommunity>
                        <ns:EnergyCommunity>
                            <ns:mRID>AT00100000000RC100123000000123456</ns:mRID>
                            <ns:dateFrom>2022-11-14Z</ns:dateFrom>
                            <ns:AccountingPoint>
                                <ns:mRID codingScheme="NAT">AT0010000103600000000123456123459</ns:mRID>
                                <ns:energySharingParticipationFactor>100</ns:energySharingParticipationFactor>
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
    }
}