// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntermediateVHDFactoryTest {

    @Test
    void testCreate_returnsValidatedHistoricalDocument() throws IOException {
        // Given
        DatadisConfiguration datadisConfig = new DatadisConfiguration("clientId",
                                                                      "clientSecret",
                                                                      "basepath"
        );
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
