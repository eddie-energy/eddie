// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.si.moj.elektro;

import org.junit.jupiter.api.Test;

import java.time.Period;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MojElektroRegionConnectorMetadataTest {

    private final MojElektroRegionConnectorMetadata metadata = new MojElektroRegionConnectorMetadata();

    @Test
    void testId() {
        assertEquals("si-moj-elektro", metadata.id());
    }

    @Test
    void testCountryCode() {
        assertEquals("SI", metadata.countryCode());
    }

    @Test
    void testCoveredMeteringPoints() {
        assertEquals(0, metadata.coveredMeteringPoints());
    }

    @Test
    void testEarliestStart() {
        assertEquals(Period.ZERO, metadata.earliestStart());
    }

    @Test
    void testLatestEnd() {
        assertEquals(Period.ZERO, metadata.latestEnd());
    }

    @Test
    void testTimeZone() {
        assertEquals(ZoneOffset.UTC, metadata.timeZone());
    }
}
