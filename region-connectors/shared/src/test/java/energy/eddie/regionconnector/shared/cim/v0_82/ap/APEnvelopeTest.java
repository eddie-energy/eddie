package energy.eddie.regionconnector.shared.cim.v0_82.ap;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.ap.AccountingPointMarketDocumentComplexType;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class APEnvelopeTest {

    @Test
    void wrapReturnsEnvelope() {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
        var ap = new AccountingPointMarketDocumentComplexType();
        var envelope = new APEnvelope(ap, pr);

        // When
        var res = envelope.wrap();

        // Then
        var header = res.getMessageDocumentHeader().getMessageDocumentHeaderMetaInformation();
        assertAll(
                () -> assertEquals(ap, res.getAccountingPointMarketDocument()),
                () -> assertEquals("pid", header.getPermissionid()),
                () -> assertEquals("cid", header.getConnectionid()),
                () -> assertEquals("dnid", header.getDataNeedid()),
                () -> assertEquals(DocumentType.ACCOUNTING_POINT_MARKET_DOCUMENT.description(),
                                   header.getDataType())
        );
    }
}