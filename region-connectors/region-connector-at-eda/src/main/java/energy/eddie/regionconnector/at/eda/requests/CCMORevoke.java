package energy.eddie.regionconnector.at.eda.requests;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.MarketParticipantDirectory;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class CCMORevoke {
    private final AtPermissionRequest permissionRequest;
    private final String eligiblePartyId;

    public CCMORevoke(AtPermissionRequest permissionRequest, String eligiblePartyId) {
        this.permissionRequest = permissionRequest;
        this.eligiblePartyId = eligiblePartyId;
    }

    public CMRevoke toCMRevoke() {
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
                                                .withReceiver(new CCMOAddress(permissionRequest.dataSourceInformation().permissionAdministratorId()).toRoutingAddress())
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
                                .withMessageId(new MessageId(new CCMOAddress(eligiblePartyId).toRoutingAddress(),
                                                             ZonedDateTime.now(
                                                                     EdaRegionConnectorMetadata.AT_ZONE_ID)).toString())
                                .withConversationId(permissionRequest.conversationId())
                                .withConsentEnd(
                                        permissionRequest.end() == null
                                                ? null
                                                : DateTimeConverter.dateTimeToXml(permissionRequest.end())
                                )
                                .withReason("Terminated by EP")
                );
    }
}
