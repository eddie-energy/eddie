package energy.eddie.aiida.datasources.simulation.configs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

class SimDataSourceConfigTest {
    @Test
    void testHashCode_And_Equals() {
        // given
        var simDataSourceConfig1 = new SimDataSourceConfig(true, "1", 3);
        var simDataSourceConfig2 = new SimDataSourceConfig(true, "1", 5);
        var simDataSourceConfig3 = new SimDataSourceConfig(true, "2", 5);

        // when
        var simDataSourceConfigSet = new HashSet<SimDataSourceConfig>();
        simDataSourceConfigSet.add(simDataSourceConfig1);
        simDataSourceConfigSet.add(simDataSourceConfig2);
        simDataSourceConfigSet.add(simDataSourceConfig3);

        // then
        Assertions.assertEquals(2, simDataSourceConfigSet.size());
        Assertions.assertEquals(simDataSourceConfig1, simDataSourceConfig2);
        Assertions.assertNotEquals(simDataSourceConfig2, simDataSourceConfig3);
    }
}
