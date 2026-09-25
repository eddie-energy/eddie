// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p10;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p10.CMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevokeImpl;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

@Mapper
public interface CMRevokeMapper {

    CMRevokeMapper INSTANCE = Mappers.getMapper(CMRevokeMapper.class);

    @Mapping(target = "meteringPoint", source = "processDirectory.meteringPoint")
    @Mapping(target = "consentId", source = "processDirectory.consentId")
    @Mapping(target = "consentEnd", source = "processDirectory.consentEnd", qualifiedByName = "consentEnd")
    EdaCMRevokeImpl toEdaCMRevoke(CMRevoke cmRevoke);

    @Named("consentEnd")
    default LocalDate consentEnd(XMLGregorianCalendar end) {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(end).toLocalDate();
    }
}
