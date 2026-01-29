// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v0_82.vhd;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VhdEnvelopeTest {
    @Test
    void wrapReturnsEnvelope() {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
        var vhd = new ValidatedHistoricalDataMarketDocumentComplexType();
        var envelope = new VhdEnvelope(vhd, pr);

        // When
        var res = envelope.wrap();

        // Then
        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        assertAll(
                () -> assertEquals(vhd, res.getValidatedHistoricalDataMarketDocument()),
                () -> assertEquals("pid", header.getPermissionid()),
                () -> assertEquals("cid", header.getConnectionid()),
                () -> assertEquals("dnid", header.getDataNeedid())
        );
    }
}