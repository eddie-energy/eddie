package energy.eddie.aiida.datasources.fr.configs.dtos;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

class MicroTeleinfoV3DatasourceTest {
    @Test
    void testHashCode_And_Equals() {
        // given
        var frDataSourceConfig1 = new MicroTeleinfoV3Datasource(true,
                                                                "1",
                                                                "tcp://example1.com",
                                                                "test1",
                                                                "",
                                                                "",
                                                                "meterId");
        var frDataSourceConfig2 = new MicroTeleinfoV3Datasource(true,
                                                                "1",
                                                                "tcp://example2.com",
                                                                "test2",
                                                                "",
                                                                "",
                                                                "meterId");
        var frDataSourceConfig3 = new MicroTeleinfoV3Datasource(true,
                                                                "2",
                                                                "tcp://example2.com",
                                                                "test2",
                                                                "",
                                                                "",
                                                                "meterId");

        // when
        var frDataSourceConfigSet = new HashSet<MicroTeleinfoV3Datasource>();
        frDataSourceConfigSet.add(frDataSourceConfig1);
        frDataSourceConfigSet.add(frDataSourceConfig2);
        frDataSourceConfigSet.add(frDataSourceConfig3);

        // then
        Assertions.assertEquals(2, frDataSourceConfigSet.size());
        Assertions.assertEquals(frDataSourceConfig1, frDataSourceConfig2);
        Assertions.assertNotEquals(frDataSourceConfig2, frDataSourceConfig3);
    }
}
