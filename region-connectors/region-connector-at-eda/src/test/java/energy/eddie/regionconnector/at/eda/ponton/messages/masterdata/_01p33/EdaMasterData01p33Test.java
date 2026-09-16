// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import at.ebutilities.schemata.customerprocesses.masterdata._01p33.*;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.MasterDataMapper;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.NullMeteringPointData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class EdaMasterData01p33Test {
    @Test
    void testMeteringPointData_withMeteringPointData_returnsNonNullObject() {
        // Given
        var masterData = MasterDataMapper.INSTANCE.toEdaMasterData(
                new MasterData()
                        .withProcessDirectory(
                                new ProcessDirectory()
                                        .withMeteringPointData(
                                                new MeteringPointData()
                                                        .withSupStatus(SupStatus.ON)
                                        )
                        )
        );

        // When
        var res = masterData.meteringPointData();

        // Then
        assertNotNull(res.supStatus());
    }

    @Test
    void testMeteringPointData_withoutMeteringPointData_returnsNullObject() {
        // Given
        var masterData = MasterDataMapper.INSTANCE.toEdaMasterData(
                new MasterData()
                        .withProcessDirectory(
                                new ProcessDirectory()
                        )
        );

        // When
        var res = masterData.meteringPointData();

        // Then
        assertEquals(new NullMeteringPointData(), res);
    }

    @Test
    void testContractPartner_withoutDateOfBirth_returnsNullDateOfBirth() {
        // Given
        var masterData = MasterDataMapper.INSTANCE.toEdaMasterData(
                new MasterData()
                        .withProcessDirectory(
                                new ProcessDirectory()
                                        .withContractPartner(new ContractPartner())
                        )
        );

        // When
        var res = masterData.contractPartner();

        // Then
        assertThat(res)
                .isPresent()
                .get()
                .satisfies(r -> {
                    assertNull(r.dateOfBirth());
                    assertNull(r.dateOfDeath());
                });
    }
}