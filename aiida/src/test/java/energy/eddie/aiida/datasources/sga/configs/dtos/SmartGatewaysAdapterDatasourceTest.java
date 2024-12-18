package energy.eddie.aiida.datasources.sga.configs.dtos;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class SmartGatewaysAdapterDatasourceTest {
    @Test
    void testHashCode_And_Equals() {
        // given
        var sgaDataSourceConfig1 = new SmartGatewaysAdapterDatasource(true,
                                                                "1",
                                                                "tcp://example1.com",
                                                                "test1",
                                                                "",
                                                                "");
        var sgaDataSourceConfig2 = new SmartGatewaysAdapterDatasource(true,
                                                                "1",
                                                                "tcp://example2.com",
                                                                "test2",
                                                                "",
                                                                "");
        var sgaDataSourceConfig3 = new SmartGatewaysAdapterDatasource(true,
                                                                "2",
                                                                "tcp://example2.com",
                                                                "test2",
                                                                "",
                                                                "");

        // when
        var sgaDataSourceConfigSet = new HashSet<SmartGatewaysAdapterDatasource>();
        sgaDataSourceConfigSet.add(sgaDataSourceConfig1);
        sgaDataSourceConfigSet.add(sgaDataSourceConfig2);
        sgaDataSourceConfigSet.add(sgaDataSourceConfig3);

        // then
        Assertions.assertEquals(2, sgaDataSourceConfigSet.size());
        Assertions.assertEquals(sgaDataSourceConfig1, sgaDataSourceConfig2);
        Assertions.assertNotEquals(sgaDataSourceConfig2, sgaDataSourceConfig3);
    }
}
