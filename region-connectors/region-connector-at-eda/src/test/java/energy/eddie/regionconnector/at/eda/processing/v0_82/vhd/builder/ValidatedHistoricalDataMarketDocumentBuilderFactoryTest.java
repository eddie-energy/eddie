// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValidatedHistoricalDataMarketDocumentBuilderFactoryTest {

    @Test
    void create_returnsBuilder() {
        assertNotNull(new ValidatedHistoricalDataMarketDocumentBuilderFactory().create());
    }
}