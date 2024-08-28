package energy.eddie.regionconnector.shared.cim.v0_82.ap;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocumentComplexType;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class APEnvelopeTest {

    @Test
    void wrapReturnsEnveloppe() {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
        var ap = new AccountingPointMarketDocumentComplexType();
        var enveloppe = new APEnvelope(ap, pr);

        // When
        var res = enveloppe.wrap();

        // Then
        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        assertAll(
                () -> assertEquals(ap, res.getAccountingPointMarketDocument()),
                () -> assertEquals("pid", header.getPermissionid()),
                () -> assertEquals("cid", header.getConnectionid()),
                () -> assertEquals("dnid", header.getDataNeedid())
        );
    }
}