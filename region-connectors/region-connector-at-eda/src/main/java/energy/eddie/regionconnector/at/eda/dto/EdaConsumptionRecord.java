// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public interface EdaConsumptionRecord {
    String messageId();

    String conversationId();

    String meteringPoint();

    LocalDate startDate();

    LocalDate endDate();

    String senderMessageAddress();

    ZonedDateTime documentCreationDateTime();

    String receiverMessageAddress();

    List<Energy> energy();

    String schemaVersion();

    XMLGregorianCalendar processDate();

    Object originalConsumptionRecord();
}
