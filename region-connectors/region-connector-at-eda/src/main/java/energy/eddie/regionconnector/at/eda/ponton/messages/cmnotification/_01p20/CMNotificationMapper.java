// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p20;

import at.ebutilities.schemata.customerconsent.cmnotification._01p20.CMNotification;
import at.ebutilities.schemata.customerconsent.cmnotification._01p20.ResponseDataType;
import energy.eddie.regionconnector.at.eda.dto.EdaCMNotificationImpl;
import energy.eddie.regionconnector.at.eda.dto.ResponseDataImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CMNotificationMapper {

    CMNotificationMapper INSTANCE = Mappers.getMapper(CMNotificationMapper.class);

    @Mapping(target = "conversationId", source = "processDirectory.conversationId")
    @Mapping(target = "cmRequestId", source = "processDirectory.CMRequestId")
    @Mapping(target = "responseData", source = "processDirectory.responseData")
    EdaCMNotificationImpl toEdaCMNotification(CMNotification notification);

    @Mapping(target = "responseCodes", source = "responseCode")
    ResponseDataImpl toResponseData(ResponseDataType responseData);
}
