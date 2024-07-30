package energy.eddie.aiida.datasources.at.configs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

class AtDataSourceConfigTest {
    @Test
    void testHashCode_And_Equals() {
        // given
        var atDataSourceConfig1 = new AtDataSourceConfig(true, "1", "tcp://example1.com", "test1", "", "");
        var atDataSourceConfig2 = new AtDataSourceConfig(true, "1", "tcp://example2.com", "test2", "", "");
        var atDataSourceConfig3 = new AtDataSourceConfig(true, "2", "tcp://example2.com", "test2", "", "");

        // when
        var atDataSourceConfigSet = new HashSet<AtDataSourceConfig>();
        atDataSourceConfigSet.add(atDataSourceConfig1);
        atDataSourceConfigSet.add(atDataSourceConfig2);
        atDataSourceConfigSet.add(atDataSourceConfig3);

        // then
        Assertions.assertEquals(2, atDataSourceConfigSet.size());
        Assertions.assertEquals(atDataSourceConfig1, atDataSourceConfig2);
        Assertions.assertNotEquals(atDataSourceConfig2, atDataSourceConfig3);
    }
}
