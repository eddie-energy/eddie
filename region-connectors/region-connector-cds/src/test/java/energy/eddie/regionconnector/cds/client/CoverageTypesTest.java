// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner.CommodityTypesEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoverageTypesTest {
    @Test
    void toCoverages_returnsCorrectCoverageTypes() {
        // Given
        var input = List.of(
                new Coverages200ResponseAllOfCoverageEntriesInner()
                        .country("us")
                        .commodityTypes(List.of(CommodityTypesEnum.ELECTRICITY,
                                                CommodityTypesEnum.NATURAL_GAS)),
                new Coverages200ResponseAllOfCoverageEntriesInner()
                        .country("at")
                        .commodityTypes(List.of(CommodityTypesEnum.TRASH))
        );
        var coverages = new CoverageTypes(input);

        // When
        var res = coverages.toCoverages();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new Coverage(EnergyType.ELECTRICITY, "us"),
                        new Coverage(EnergyType.NATURAL_GAS, "us")
                );
    }
}