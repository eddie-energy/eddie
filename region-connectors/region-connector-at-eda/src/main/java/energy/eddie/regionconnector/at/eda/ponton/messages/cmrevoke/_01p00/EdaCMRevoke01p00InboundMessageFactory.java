// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke.EdaCMRevokeInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00.CMRevoke01p00OutboundMessageFactory.ACTIVE_UNTIL;

@Component
@SuppressWarnings("DuplicatedCode")
public class EdaCMRevoke01p00InboundMessageFactory implements EdaCMRevokeInboundMessageFactory {
    private final Jaxb2Marshaller marshaller;

    public EdaCMRevoke01p00InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !date.isAfter(ACTIVE_UNTIL);
    }

    @Override
    public EdaCMRevoke parseInputStream(InputStream inputStream) {
        var cmRevoke = (CMRevoke) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaCMRevoke01p00(cmRevoke);
    }
}
