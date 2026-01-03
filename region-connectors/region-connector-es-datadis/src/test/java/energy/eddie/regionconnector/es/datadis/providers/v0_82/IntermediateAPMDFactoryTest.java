package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntermediateAPMDFactoryTest {
    @Test
    void testCreate_returnsAccountingPointMarketDocument() throws IOException {
        // Given
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId", "clientSecret", "basepath");
        IntermediateAPMDFactory factory = new IntermediateAPMDFactory(
                datadisConfig,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );

        // When
        var res = factory.create(IntermediateAccountingPointMarketDocumentTest.identifiableAccountingPointData());

        // Then
        assertNotNull(res);
    }
}
