package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntermediateVHDFactoryTest {

    @Test
    void testCreate_returnsValidatedHistoricalDocument() throws IOException {
        // Given
        PlainDatadisConfiguration datadisConfig = new PlainDatadisConfiguration("clientId",
                                                                                "clientSecret",
                                                                                "basepath",
                                                                                24);
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                datadisConfig,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );

        // When
        var res = factory.create(IntermediateValidatedHistoricalDocumentTest.identifiableMeterReading(false));

        // Then
        assertNotNull(res);
    }
}
