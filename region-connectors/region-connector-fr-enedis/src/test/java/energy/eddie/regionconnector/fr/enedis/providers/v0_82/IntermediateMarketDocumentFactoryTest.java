package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.readings.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class IntermediateMarketDocumentFactoryTest {
    @Test
    void testCreate_returnsValidatedHistoricalDocument() {
        // Given
        PlainEnedisConfiguration enedisConfiguration = new PlainEnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path"
        );
        IntermediateMarketDocumentFactory factory = new IntermediateMarketDocumentFactory(
                enedisConfiguration,
                new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                             "fallbackId")
        );
        IdentifiableMeterReading meterReading = new IdentifiableMeterReading(mock(FrEnedisPermissionRequest.class),
                                                                             mock(MeterReading.class),
                                                                             MeterReadingType.CONSUMPTION);

        // When
        var res = factory.create(meterReading);

        // Then
        assertNotNull(res);
    }
}
