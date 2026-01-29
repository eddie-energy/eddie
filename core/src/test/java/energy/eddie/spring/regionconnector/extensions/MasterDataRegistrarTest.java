// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.core.services.MasterDataService;
import energy.eddie.core.services.MockMasterData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataRegistrarTest {

    @Test
    void givenNull_constructor_throws() {
        // Given
        var service = new MasterDataService();
        var masterData = new MockMasterData(
                List.of(), List.of()
        );

        // When
        new MasterDataRegistrar(Optional.of(masterData), service);

        // Then
        assertThat(service.masterData())
                .hasSize(1);
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var service = new MasterDataService();

        // When
        new MasterDataRegistrar(Optional.empty(), service);

        // Then
        assertThat(service.masterData())
                .isEmpty();
    }
}