package energy.eddie.regionconnector.at.eda;

import org.junit.jupiter.api.BeforeEach;

public class InMemoryEdaIdMapperTest extends EdaIdMapperTest {

    @BeforeEach
    void setUp() {
        edaIdMapper = new InMemoryEdaIdMapper();
    }
}
