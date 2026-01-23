// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.services;

import energy.eddie.cim.v0_82.pmd.*;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.assertArg;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatusMessageServiceTest {
    @Mock
    private StatusMessageRepository statusMessageRepository;
    @InjectMocks
    private StatusMessageService statusMessageService;

    @Test
    void testReceivesValidPermissionMarketDocument_saves() {
        // Given
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("mrid")
                                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                                           .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                                                           .withValue("Enedis"))
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withCreatedDateTime(
                                                                                                                                                "2021-01-01T00:00:00Z")
                                                                                                                                        .withStatus(
                                                                                                                                                StatusTypeList.A05)
                                                                                                                        )
                                                                                     )
                                                            )
                                )
                );
        // When
        statusMessageService.processMessage(pmd);
        // Then
        verify(statusMessageRepository)
                .save(assertArg(message -> assertAll(
                        () -> assertEquals("mrid", message.getPermissionId()),
                        () -> assertEquals("NFR", message.getCountry()),
                        () -> assertEquals("Enedis", message.getDso()),
                        () -> assertEquals("2021-01-01T00:00:00Z", message.getStartDate()),
                        () -> assertEquals("A05", message.getStatus())
                )));
    }

    @Test
    void testReceivesAiidaPermissionMarketDocument() {
        // Given
        var pmd = new PermissionEnvelope()
                .withPermissionMarketDocument(
                        new PermissionMarketDocumentComplexType()
                                .withMRID("mrid")
                                .withReceiverMarketParticipantMRID(new PartyIDStringComplexType()
                                                                           .withCodingScheme(null)
                                                                           .withValue("Aiida"))
                                .withType(MessageTypeList.PERMISSION_TERMINATION_DOCUMENT)
                                .withPermissionList(new PermissionMarketDocumentComplexType.PermissionList()
                                                            .withPermissions(new PermissionComplexType()
                                                                                     .withMktActivityRecordList(new PermissionComplexType.MktActivityRecordList()
                                                                                                                        .withMktActivityRecords(
                                                                                                                                new MktActivityRecordComplexType()
                                                                                                                                        .withCreatedDateTime(
                                                                                                                                                "2021-01-01T00:00:00Z")
                                                                                                                                        .withStatus(
                                                                                                                                                StatusTypeList.A05)
                                                                                                                        )
                                                                                     )
                                                            )
                                )
                );
        // When
        statusMessageService.processMessage(pmd);

        // Then
        verify(statusMessageRepository)
                .save(assertArg(message -> assertAll(
                        () -> assertEquals("mrid", message.getPermissionId()),
                        () -> assertEquals("Unknown", message.getCountry()),
                        () -> assertEquals("Aiida", message.getDso()),
                        () -> assertEquals("2021-01-01T00:00:00Z", message.getStartDate()),
                        () -> assertEquals("A05", message.getStatus())
                )));
    }
}