// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CimTestConfiguration {
    @Bean
    public CommonInformationModelConfiguration commonInformationModelConfiguration() {
        return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME,
                                                            "EP-ID");
    }
}
