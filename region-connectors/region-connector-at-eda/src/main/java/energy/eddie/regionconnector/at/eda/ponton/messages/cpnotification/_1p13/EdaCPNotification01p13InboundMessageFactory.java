// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification._1p13;

import at.ebutilities.schemata.customerprocesses.cpnotification._01p13.CPNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCPNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification.EdaCPNotificationInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

@Component
@SuppressWarnings("DuplicatedCode")
public class EdaCPNotification01p13InboundMessageFactory implements EdaCPNotificationInboundMessageFactory {
    private final Jaxb2Marshaller marshaller;

    public EdaCPNotification01p13InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public EdaCPNotification parseInputStream(InputStream inputStream) {
        var notification = (CPNotification) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaCPNotification01p13(notification);
    }

    @Override
    public boolean isActive(LocalDate date) {
        return true;
    }
}
