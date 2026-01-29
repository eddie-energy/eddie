// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim.v0_82.pmd;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList;
import energy.eddie.regionconnector.shared.cim.v0_82.DocumentType;
import energy.eddie.regionconnector.shared.permission.requests.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentHeaderTest {

    @Test
    void headerCreatesDocumentPermissionMarketDocumentHeader() {
        // Given
        var pr = new SimplePermissionRequest("pid", "cid", "dnid", PermissionProcessStatus.ACCEPTED);
        var documentType = DocumentType.PERMISSION_MARKET_DOCUMENT;
        var header = new DocumentHeader(pr, documentType);

        // When
        var res = header.permissionMarketDocumentHeader();

        // Then
        var info = res.getMessageDocumentHeaderMetaInformation();
        var headerRegion = info.getMessageDocumentHeaderRegion();
        assertAll(
                () -> assertNotNull(res.getCreationDateTime()),
                () -> assertEquals("cid", info.getConnectionid()),
                () -> assertEquals("dnid", info.getDataNeedid()),
                () -> assertEquals(documentType.description(), info.getDataType()),
                () -> assertEquals("dummy-rc", headerRegion.getConnector()),
                () -> assertEquals(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, headerRegion.getCountry())
        );
    }
}