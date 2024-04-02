package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p20;

import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactoryTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CMRequest01p20OutboundMessageFactoryTest extends CMRequestOutboundMessageFactoryTest {


    @Test
    void isActive_on_07_04_2024_returnsFalse() {
        // given
        var factory = new CMRequest01p20OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 7));

        // then
        assertFalse(active);
    }

    @Test
    void isActive_on_08_04_2024_returnsTrue() {
        // given
        var factory = new CMRequest01p20OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 8));

        // then
        assertTrue(active);
    }

    @Override
    protected CMRequestOutboundMessageFactory factory() {
        return new CMRequest01p20OutboundMessageFactory(marshaller);
    }
}
