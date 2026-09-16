// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering;

import org.junit.jupiter.api.Test;

import java.time.Period;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EleringRegionConnectorMetadataTest {

    private final EleringRegionConnectorMetadata metadata = new EleringRegionConnectorMetadata();

    @Test
    void testId() {
        assertEquals("ee-elering", metadata.id());
    }

    @Test
    void testCountryCode() {
        assertEquals("EE", metadata.countryCode());
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
