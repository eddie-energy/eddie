// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.vhd.TimeSeries;
import energy.eddie.cim.v1_12.vhd.VHDMarketDocument;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class VhdEnvelopeWrapperTest {
    private final SimplePermissionRequest permission = new SimplePermissionRequest(
            "pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
    private final ZonedDateTime created = ZonedDateTime.parse("2026-01-02T03:04:05Z");

    @Test
    void fillsRequiredProcessTimeFromDocumentAndKeepsOptionalFieldsAbsent() {
        var process = created.plusDays(1);
        var fallback = new TimeSeries();
        var supplied = new TimeSeries().withDateAndOrTimeDateTime(process);
        var document = new VHDMarketDocument().withRevisionNumber("104")
                                              .withCreatedDateTime(created)
                                              .withTimeSeries(fallback, supplied);

        var result = new VhdEnvelopeWrapper(document, permission, created.plusSeconds(1)).wrap();

        assertSame(document, result.getVHDMarketDocument());
        assertEquals(created.plusSeconds(1), result.getMessageDocumentHeader().getCreationDateTime());
        assertEquals("pid", result.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
        assertEquals("cid", result.getMessageDocumentHeader().getMetaInformation().getConnectionId());
        assertEquals("dnid", result.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
        assertEquals("104", document.getRevisionNumber());
        assertEquals(created, fallback.getDateAndOrTimeDateTime());
        assertEquals(process, supplied.getDateAndOrTimeDateTime());
        assertNull(fallback.getMRID());
        assertNull(fallback.getAccountingPointMRID());
        assertNull(fallback.getEnergyMeasurementUnitName());
        assertTrue(fallback.getSeriesPeriods().isEmpty());
    }

    @Test
    void preservesHeaderOnlyEnvelope() {
        var result = new VhdEnvelopeWrapper(null, permission, created).wrap();

        assertNull(result.getVHDMarketDocument());
        assertEquals(created, result.getMessageDocumentHeader().getCreationDateTime());
        assertEquals("pid", result.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
    }

    @Test
    void legacyWrapperAcceptsCorrelatedTimeAndRetainsItsDefaultConstructor() {
        var document = new energy.eddie.cim.v1_04.vhd.VHDMarketDocument();
        var explicit = new energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper(document, permission, created).wrap();
        var before = ZonedDateTime.now(ZoneOffset.UTC);
        var defaultTime = new energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper(document, permission).wrap();
        var after = ZonedDateTime.now(ZoneOffset.UTC);

        assertSame(document, explicit.getMarketDocument());
        assertEquals(created, explicit.getMessageDocumentHeaderCreationDateTime());
        assertFalse(defaultTime.getMessageDocumentHeaderCreationDateTime().isBefore(before));
        assertFalse(defaultTime.getMessageDocumentHeaderCreationDateTime().isAfter(after));
        assertEquals("pid", defaultTime.getMessageDocumentHeaderMetaInformationPermissionId());
    }

    @Test
    void legacyDefaultWrapperCreatesTimeOnEveryWrapWhileExplicitTimeRemainsFixed() {
        var later = created.plusSeconds(1);
        var document = new energy.eddie.cim.v1_04.vhd.VHDMarketDocument();
        try (var clock = mockStatic(ZonedDateTime.class)) {
            clock.when(() -> ZonedDateTime.now(ZoneOffset.UTC)).thenReturn(created, later);
            var defaultWrapper = new energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper(document, permission);
            var explicitWrapper = new energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper(document, permission, created);

            assertEquals(created, defaultWrapper.wrap().getMessageDocumentHeaderCreationDateTime());
            assertEquals(later, defaultWrapper.wrap().getMessageDocumentHeaderCreationDateTime());
            assertEquals(created, explicitWrapper.wrap().getMessageDocumentHeaderCreationDateTime());
            assertEquals(created, explicitWrapper.wrap().getMessageDocumentHeaderCreationDateTime());
        }
    }
}
