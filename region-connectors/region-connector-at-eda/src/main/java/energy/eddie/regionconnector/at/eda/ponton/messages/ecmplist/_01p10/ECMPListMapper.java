// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.ECMPList;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPListData;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPTimeData;
import energy.eddie.api.agnostic.data.needs.EnergyDirection;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EdaECMPListImpl;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EnergyCommunityMeteringPointDataImpl;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.MeteringPointTimeDataImpl;
import jakarta.annotation.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;

@Mapper
public interface ECMPListMapper {

    ECMPListMapper INSTANCE = Mappers.getMapper(ECMPListMapper.class);

    @Mapping(target = "messageId", source = "processDirectory.messageId")
    @Mapping(target = "conversationId", source = "processDirectory.conversationId")
    @Mapping(target = "ecId", source = "processDirectory.ECID")
    @Mapping(target = "senderMessageAddress", source = "marketParticipantDirectory.routingHeader.sender.messageAddress")
    @Mapping(target = "receiverMessageAddress", source = "marketParticipantDirectory.routingHeader.receiver.messageAddress")
    @Mapping(target = "documentCreationDateTime", source = "marketParticipantDirectory.routingHeader.documentCreationDateTime", qualifiedByName = "toZonedDateTime")
    @Mapping(target = "mpListData", source = "processDirectory.MPListData")
    @Mapping(target = "original", source = "ecmpList", qualifiedByName = "original")
    EdaECMPListImpl toEdaECMPList(ECMPList ecmpList);

    @Mapping(target = "meteringPoint", source = "meteringPoint")
    @Mapping(target = "timeData", source = "MPTimeData")
    EnergyCommunityMeteringPointDataImpl toMeteringPointData(MPListData mpListData);

    @Mapping(target = "dateFrom", source = "dateFrom", qualifiedByName = "toZonedDateTime")
    @Mapping(target = "energyDirection", source = "energyDirection", qualifiedByName = "energyDirection")
    @Mapping(target = "ecPartFact", source = "ECPartFact", qualifiedByName = "toBigInteger")
    MeteringPointTimeDataImpl toTimeData(MPTimeData mpTimeData);

    @Named("toZonedDateTime")
    @Nullable
    default ZonedDateTime toZonedDateTime(@Nullable XMLGregorianCalendar calendar) {
        return calendar == null ? null : calendar.toGregorianCalendar().toZonedDateTime();
    }

    @Named("original")
    default Object original(Object ecmpList) {
        return ecmpList;
    }

    @Named("energyDirection")
    @Nullable
    default EnergyDirection energyDirection(@Nullable at.ebutilities.schemata.customerprocesses.ecmplist._01p10.EnergyDirection energyDirection) {
        if (energyDirection == null) {
            return null;
        }
        return switch (energyDirection) {
            case CONSUMPTION -> EnergyDirection.CONSUMPTION;
            case GENERATION -> EnergyDirection.PRODUCTION;
        };
    }

    @Named("toBigInteger")
    @Nullable
    default BigInteger toBigInteger(@Nullable BigDecimal value) {
        return value == null ? null : value.toBigInteger();
    }
}
