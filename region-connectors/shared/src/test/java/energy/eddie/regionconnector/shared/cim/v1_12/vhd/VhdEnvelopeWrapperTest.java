// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v1_12.vhd;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.v1_12.vhd.VHDMarketDocument;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VhdEnvelopeWrapperTest {
    @Test
    void wrap_setsHeaderFromPermissionRequest() {
        // Given
        var clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        var dataSourceInformation = mock(DataSourceInformation.class);
        when(dataSourceInformation.regionConnectorId()).thenReturn("at-eda");
        when(dataSourceInformation.countryCode()).thenReturn("at");
        var permissionRequest = mock(PermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.dataSourceInformation()).thenReturn(dataSourceInformation);
        var document = new VHDMarketDocument().withMRID("mrid");

        // When
        var res = new VhdEnvelopeWrapper(document, permissionRequest, clock).wrap();

        // Then
        var header = res.getMessageDocumentHeader();
        var meta = header.getMetaInformation();
        assertAll(
                () -> assertSame(document, res.getVHDMarketDocument()),
                () -> assertEquals(ZonedDateTime.parse("2026-01-01T10:00:00Z"), header.getCreationDateTime()),
                () -> assertEquals("pid", meta.getRequestPermissionId()),
                () -> assertEquals("cid", meta.getConnectionId()),
                () -> assertEquals("dnid", meta.getDataNeedId()),
                () -> assertEquals("validated-historical-data-market-document", meta.getDocumentType()),
                () -> assertEquals("at-eda", meta.getRegionConnector()),
                () -> assertEquals("AT", meta.getRegionCountry())
        );
    }
}
