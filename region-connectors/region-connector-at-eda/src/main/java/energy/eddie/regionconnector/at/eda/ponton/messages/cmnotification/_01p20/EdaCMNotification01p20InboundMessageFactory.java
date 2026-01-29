// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p20;

import at.ebutilities.schemata.customerconsent.cmnotification._01p20.CMNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification.EdaCMNotificationInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

@Component
@SuppressWarnings("DuplicatedCode")
public class EdaCMNotification01p20InboundMessageFactory implements EdaCMNotificationInboundMessageFactory {
    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/296">ebutilities</a>
     */
    public static final LocalDate ACTIVE_FROM = LocalDate.of(2026, 4, 13);
    private final Jaxb2Marshaller marshaller;


    public EdaCMNotification01p20InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !date.isBefore(ACTIVE_FROM);
    }

    @Override
    public EdaCMNotification parseInputStream(InputStream inputStream) {
        var notification = (CMNotification) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaCMNotification01p20(notification);
    }
}
