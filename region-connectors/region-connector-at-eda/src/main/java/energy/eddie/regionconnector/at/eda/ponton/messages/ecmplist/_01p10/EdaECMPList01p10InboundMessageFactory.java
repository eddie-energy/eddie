// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.ECMPList;
import energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist.EdaECMPListInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

@Component
@SuppressWarnings("DuplicatedCode")
public class EdaECMPList01p10InboundMessageFactory implements EdaECMPListInboundMessageFactory {
    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/160">ebutilities</a>
     */
    public static final LocalDate ACTIVE_FROM = LocalDate.of(2024, 4, 8);
    private final Jaxb2Marshaller marshaller;


    public EdaECMPList01p10InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !date.isBefore(ACTIVE_FROM);
    }

    @Override
    public ECMPList01p10 parseInputStream(InputStream inputStream) {
        var notification = (ECMPList) marshaller.unmarshal(new StreamSource(inputStream));
        return new ECMPList01p10(notification);
    }
}
