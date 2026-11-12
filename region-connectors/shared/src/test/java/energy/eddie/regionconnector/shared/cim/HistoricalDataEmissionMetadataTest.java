// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class HistoricalDataEmissionMetadataTest {
    @Test
    void identifiersDependOnRawKeysWithoutDependingOnMappingOrder() {
        var metadata = new HistoricalDataEmissionMetadata();
        var first = metadata.child("account:0/meter:0/segment:0");
        var second = metadata.child("account:0/meter:0/segment:1");
        var secondSeries = second.id("format:KWH_FWD");
        var firstSeries = first.id("format:KWH_FWD");

        assertEquals(firstSeries, metadata.child("account:0/meter:0/segment:0").id("format:KWH_FWD"));
        assertEquals(secondSeries, metadata.child("account:0/meter:0/segment:1").id("format:KWH_FWD"));
        assertNotEquals(firstSeries, secondSeries);
        assertNotEquals(firstSeries, first.id("document"));
        assertNotEquals(firstSeries, first.id("format:KWH_REV"));
        assertNotEquals(metadata.child("a/b").id("series"), metadata.child("a").child("b").id("series"));
    }

    @Test
    void eachEmissionHasIndependentIdentifiersAndOneCreationInstant() {
        var metadata = new HistoricalDataEmissionMetadata();
        var laterEmission = new HistoricalDataEmissionMetadata();

        assertNotEquals(metadata.id("document"), laterEmission.id("document"));
        assertEquals(metadata.envelopeCreated(), metadata.child("permission:0").envelopeCreated());
        assertEquals(metadata.created(ZoneOffset.UTC).toInstant(), metadata.created(ZoneId.of("Europe/Vienna")).toInstant());
        assertEquals(metadata.created(ZoneOffset.UTC).toInstant(), metadata.child("account:0").created(ZoneOffset.UTC).toInstant());
    }
}
