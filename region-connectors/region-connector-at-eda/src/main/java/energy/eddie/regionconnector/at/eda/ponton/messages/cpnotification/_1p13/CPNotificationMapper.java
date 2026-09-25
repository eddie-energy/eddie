// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification._1p13;

import at.ebutilities.schemata.customerprocesses.cpnotification._01p13.CPNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCPNotificationImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CPNotificationMapper {

    CPNotificationMapper INSTANCE = Mappers.getMapper(CPNotificationMapper.class);

    @Mapping(target = "conversationId", source = "processDirectory.conversationId")
    @Mapping(target = "originalMessageId", source = "processDirectory.responseData.originalMessageID")
    @Mapping(target = "responseCodes", source = "processDirectory.responseData.responseCode")
    EdaCPNotificationImpl toEdaCPNotification(CPNotification notification);
}
