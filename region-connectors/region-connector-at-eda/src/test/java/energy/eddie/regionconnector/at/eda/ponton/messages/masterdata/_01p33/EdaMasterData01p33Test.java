// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import at.ebutilities.schemata.customerprocesses.masterdata._01p33.MasterData;
import at.ebutilities.schemata.customerprocesses.masterdata._01p33.MeteringPointData;
import at.ebutilities.schemata.customerprocesses.masterdata._01p33.ProcessDirectory;
import at.ebutilities.schemata.customerprocesses.masterdata._01p33.SupStatus;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.NullMeteringPointData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EdaMasterData01p33Test {
    @Test
    void testMeteringPointData_withMeteringPointData_returnsNonNullObject() {
        // Given
        var masterData = new EdaMasterData01p33(
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
        var masterData = new EdaMasterData01p33(
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
}