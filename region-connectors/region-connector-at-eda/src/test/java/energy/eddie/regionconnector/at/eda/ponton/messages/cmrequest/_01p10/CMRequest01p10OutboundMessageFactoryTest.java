package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p10;

import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest.CMRequestOutboundMessageFactoryTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@Import(MarshallerConfig.class)
class CMRequest01p10OutboundMessageFactoryTest extends CMRequestOutboundMessageFactoryTest {

    @Override
    protected CMRequestOutboundMessageFactory factory() {
        return new CMRequest01p10OutboundMessageFactory(marshaller);
    }

    @Test
    void isActive_on_07_04_2024_returnsTrue() {
        // given
        var factory = new CMRequest01p10OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 7));

        // then
        assertTrue(active);
    }

    @Test
    void isActive_on_08_04_2024_returnsFalse() {
        // given
        var factory = new CMRequest01p10OutboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 8));

        // then
        assertFalse(active);
    }
}
