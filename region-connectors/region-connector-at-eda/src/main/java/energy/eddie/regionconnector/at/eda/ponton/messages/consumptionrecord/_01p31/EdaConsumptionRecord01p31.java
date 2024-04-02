package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p31;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public record EdaConsumptionRecord01p31(ConsumptionRecord consumptionRecord) implements EdaConsumptionRecord {
    @Override
    public String conversationId() {
        return consumptionRecord.getProcessDirectory().getConversationId();
    }

    @Override
    public String meteringPoint() {
        return consumptionRecord.getProcessDirectory().getMeteringPoint();
    }

    @Override
    public LocalDate startDate() {
        return consumptionRecord.getProcessDirectory()
                                .getEnergy()
                                .getFirst()
                                .getMeteringPeriodStart().toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    @Override
    public LocalDate endDate() {
        return consumptionRecord.getProcessDirectory()
                                .getEnergy()
                                .getLast()
                                .getMeteringPeriodEnd().toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    @Override
    public String senderMessageAddress() {
        return consumptionRecord.getMarketParticipantDirectory().getRoutingHeader().getSender().getMessageAddress();
    }

    @Override
    public ZonedDateTime documentCreationDateTime() {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(consumptionRecord.getMarketParticipantDirectory()
                                                                             .getRoutingHeader()
                                                                             .getDocumentCreationDateTime());
    }

    @Override
    public String receiverMessageAddress() {
        return consumptionRecord.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress();
    }

    @Override
    public List<Energy> energy() {
        return consumptionRecord.getProcessDirectory().getEnergy().stream()
                                .map(energy -> (Energy) new Energy01p31(energy))
                                .toList();
    }

    @Override
    public String schemaVersion() {
        return consumptionRecord.getMarketParticipantDirectory().getSchemaVersion();
    }

    @Override
    public XMLGregorianCalendar processDate() {
        return consumptionRecord.getProcessDirectory().getProcessDate();
    }

    @Override
    public Object originalConsumptionRecord() {
        return consumptionRecord;
    }
}
