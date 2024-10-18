package energy.eddie.regionconnector.at.eda.ponton.messages.cprequest._1p12;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AddressType;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.DocumentMode;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingAddress;
import at.ebutilities.schemata.customerprocesses.common.types._01p20.RoutingHeader;
import at.ebutilities.schemata.customerprocesses.cprequest._01p12.Extension;
import at.ebutilities.schemata.customerprocesses.cprequest._01p12.MarketParticipantDirectory;
import at.ebutilities.schemata.customerprocesses.cprequest._01p12.MeteringIntervall;
import at.ebutilities.schemata.customerprocesses.cprequest._01p12.ProcessDirectory;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import energy.eddie.regionconnector.at.eda.xml.helper.Sector;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

public record CPRequest01p12(
        CPRequestCR cpRequestCR
) {
    public static final String SCHEMA_VERSION = "01.12";

    public at.ebutilities.schemata.customerprocesses.cprequest._01p12.CPRequest cpRequest() {
        return new at.ebutilities.schemata.customerprocesses.cprequest._01p12.CPRequest()
                .withMarketParticipantDirectory(makeMarketParticipantDirectory())
                .withProcessDirectory(makeProcessDirectory());
    }

    private MarketParticipantDirectory makeMarketParticipantDirectory() {
        return new MarketParticipantDirectory()
                .withMessageCode(MessageCodes.CPRequest.CODE)
                .withSector(Sector.ELECTRICITY.value())
                .withDocumentMode(DocumentMode.PROD)
                .withDuplicate(false)
                .withSchemaVersion(SCHEMA_VERSION)
                .withRoutingHeader(
                        new RoutingHeader()
                                .withSender(toRoutingAddress(cpRequestCR.eligiblePartyId()))
                                .withReceiver(toRoutingAddress(cpRequestCR.dsoId()))
                                .withDocumentCreationDateTime(
                                        DateTimeConverter.dateTimeToXml(LocalDateTime.now(AT_ZONE_ID))
                                )
                );
    }

    private static RoutingAddress toRoutingAddress(String address) {
        requireNonNull(address);
        if (address.isBlank()) {
            throw new IllegalArgumentException("Address must not be null");
        }
        return new RoutingAddress()
                .withAddressType(AddressType.EC_NUMBER)
                .withMessageAddress(address);
    }

    private ProcessDirectory makeProcessDirectory() {
        var messageId = cpRequestCR.messageId();
        String conversationID = cpRequestCR.messageId();
        return new ProcessDirectory()
                .withMessageId(messageId)
                .withConversationId(conversationID)
                .withProcessDate(DateTimeConverter.dateToXml(LocalDate.now(AT_ZONE_ID)))
                .withMeteringPoint(cpRequestCR.meteringPointId())
                .withExtension(makeExtension());
    }

    private Extension makeExtension() {
        return new Extension()
                .withDateTimeFrom(DateTimeConverter.dateTimeToXml(cpRequestCR.start().atStartOfDay(AT_ZONE_ID)))
                .withDateTimeTo(DateTimeConverter.dateTimeToXml(cpRequestCR.end().atStartOfDay(AT_ZONE_ID)))
                .withMeteringIntervall(meteringIntervall());
    }

    @Nullable
    private MeteringIntervall meteringIntervall() {
        return switch (cpRequestCR.granularity()) {
            case PT15M -> MeteringIntervall.QH;
            case P1D -> MeteringIntervall.D;
            case null -> null;
        };
    }
}
