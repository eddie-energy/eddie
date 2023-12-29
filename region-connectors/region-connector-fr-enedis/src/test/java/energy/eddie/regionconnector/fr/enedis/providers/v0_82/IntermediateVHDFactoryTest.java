package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.cim.validated_historical_data.v0_82.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntermediateVHDFactoryTest {
    @Test
    void testCreate_returnsValidatedHistoricalDocument() {
        // Given
        PlainEnedisConfiguration enedisConfiguration = new PlainEnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path"
        );
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                enedisConfiguration,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME
        );
        IdentifiableMeterReading meterReading = new IdentifiableMeterReading("pid", "cid", "dnid", new ConsumptionLoadCurveMeterReading());

        // When
        var res = factory.create(meterReading);

        // Then
        assertNotNull(res);
    }

}