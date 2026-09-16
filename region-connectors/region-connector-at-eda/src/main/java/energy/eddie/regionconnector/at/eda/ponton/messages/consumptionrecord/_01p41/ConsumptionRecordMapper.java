// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.*;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecordImpl;
import energy.eddie.regionconnector.at.eda.dto.EnergyDataImpl;
import energy.eddie.regionconnector.at.eda.dto.EnergyImpl;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import jakarta.annotation.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface ConsumptionRecordMapper {

    ConsumptionRecordMapper INSTANCE = Mappers.getMapper(ConsumptionRecordMapper.class);

    @Mapping(target = "messageId", source = "processDirectory.messageId")
    @Mapping(target = "conversationId", source = "processDirectory.conversationId")
    @Mapping(target = "meteringPoint", source = "processDirectory.meteringPoint")
    @Mapping(target = "startDate", source = "processDirectory.energy", qualifiedByName = "startDate")
    @Mapping(target = "endDate", source = "processDirectory.energy", qualifiedByName = "endDate")
    @Mapping(target = "senderMessageAddress", source = "marketParticipantDirectory.routingHeader.sender.messageAddress")
    @Mapping(target = "documentCreationDateTime", source = "marketParticipantDirectory.routingHeader.documentCreationDateTime", qualifiedByName = "toUtcZonedDateTime")
    @Mapping(target = "receiverMessageAddress", source = "marketParticipantDirectory.routingHeader.receiver.messageAddress")
    @Mapping(target = "energy", source = "processDirectory.energy")
    @Mapping(target = "schemaVersion", source = "marketParticipantDirectory.schemaVersion")
    @Mapping(target = "processDate", source = "processDirectory.processDate", qualifiedByName = "toZonedDateTime")
    @Mapping(target = "originalConsumptionRecord", source = "consumptionRecord", qualifiedByName = "originalConsumptionRecord")
    EdaConsumptionRecordImpl toEdaConsumptionRecord(ConsumptionRecord consumptionRecord);

    @Mapping(target = "granularity", source = "meteringIntervall", qualifiedByName = "granularity")
    @Mapping(target = "energyData", source = "energyData")
    @Mapping(target = "meterReadingStart", source = "meteringPeriodStart", qualifiedByName = "toUtcZonedDateTime")
    @Mapping(target = "meterReadingEnd", source = "meteringPeriodEnd", qualifiedByName = "toUtcZonedDateTime")
    @Mapping(target = "meteringReason", source = "meteringReason")
    EnergyImpl toEnergy(Energy energy);

    @Mapping(target = "energyPositions", source = "EP", qualifiedByName = "energyPositions")
    @Mapping(target = "meterCode", source = "meterCode")
    @Mapping(target = "billingUnit", source = "UOM", qualifiedByName = "billingUnit")
    EnergyDataImpl toEnergyData(EnergyData energyData);

    @Named("startDate")
    default LocalDate startDate(List<Energy> energy) {
        return energy.getFirst().getMeteringPeriodStart().toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    @Named("endDate")
    default LocalDate endDate(List<Energy> energy) {
        return energy.getLast().getMeteringPeriodEnd().toGregorianCalendar().toZonedDateTime().toLocalDate();
    }

    @Named("toUtcZonedDateTime")
    default ZonedDateTime toUtcZonedDateTime(XMLGregorianCalendar calendar) {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(calendar);
    }

    @Named("toZonedDateTime")
    default ZonedDateTime toZonedDateTime(XMLGregorianCalendar calendar) {
        return calendar.toGregorianCalendar().toZonedDateTime();
    }

    @Named("originalConsumptionRecord")
    default Object originalConsumptionRecord(Object consumptionRecord) {
        return consumptionRecord;
    }

    @Named("granularity")
    @Nullable
    default Granularity granularity(@Nullable MeteringIntervall meteringIntervall) {
        if (meteringIntervall == null) {
            return null;
        }
        return switch (meteringIntervall) {
            case QH -> Granularity.PT15M;
            case H -> Granularity.PT1H;
            case D -> Granularity.P1D;
            case V -> null;
        };
    }

    @Named("energyPositions")
    default List<EnergyPosition> energyPositions(List<at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.EnergyPosition> positions) {
        List<EnergyPosition> list = new ArrayList<>(positions.size());
        for (var position : positions) {
            list.add(new EnergyPosition(position.getBQ(), position.getMM()));
        }
        return list;
    }

    @Named("billingUnit")
    default String billingUnit(UOMType uom) {
        return uom.value();
    }
}
