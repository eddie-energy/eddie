package energy.eddie.regionconnector.shared.cim.v0_82.vhd;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VhdEnvelopeTest {
    @Test
    void wrapReturnsEnveloppe() {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
        var vhd = new ValidatedHistoricalDataMarketDocumentComplexType();
        var enveloppe = new VhdEnvelope(vhd, pr);

        // When
        var res = enveloppe.wrap();

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