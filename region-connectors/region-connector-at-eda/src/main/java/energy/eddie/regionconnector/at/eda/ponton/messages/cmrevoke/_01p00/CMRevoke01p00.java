// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.CCMOAddress;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.MessageId;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record CMRevoke01p00(
        CCMORevoke ccmoRevoke
) {
    public at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke cmRevoke() {
        var eligiblePartyId = ccmoRevoke.eligiblePartyId();
        var permissionRequest = ccmoRevoke.permissionRequest();
        String receiver = permissionRequest.dataSourceInformation()
                                           .permissionAdministratorId();
        return new CMRevoke()
                .withMarketParticipantDirectory(
                        new MarketParticipantDirectory()
                                .withDocumentMode(DocumentMode.PROD)
                                .withDuplicate(false)
                                .withMessageCode(MessageCodes.Revoke.EligibleParty.REVOKE)
                                .withSchemaVersion(MessageCodes.Revoke.VERSION)
                                .withRoutingHeader(
                                        new RoutingHeader()
                                                .withSender(new CCMOAddress(eligiblePartyId).toRoutingAddress())
                                                .withReceiver(new CCMOAddress(receiver).toRoutingAddress())
                                                .withDocumentCreationDateTime(DateTimeConverter.dateTimeToXml(
                                                        LocalDateTime.now(
                                                                EdaRegionConnectorMetadata.AT_ZONE_ID)))
                                )
                                .withSector(Sector.ELECTRICITY.value())
                )
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withMeteringPoint(permissionRequest.meteringPointId().orElse(null))
                                .withConsentId(permissionRequest.consentId().orElse(null))
                                .withMessageId(new MessageId(
                                                       eligiblePartyId,
                                                       ZonedDateTime.now(EdaRegionConnectorMetadata.AT_ZONE_ID)
                                               ).toString()
                                )
                                .withConversationId(permissionRequest.conversationId())
                                .withConsentEnd(
                                        DateTimeConverter
                                                .dateTimeToXml(
                                                        LocalDate.now(EdaRegionConnectorMetadata.AT_ZONE_ID)
                                                                 .atStartOfDay(EdaRegionConnectorMetadata.AT_ZONE_ID)
                                                )
                                )
                                .withReason(ccmoRevoke.reason())
                );
    }
}
