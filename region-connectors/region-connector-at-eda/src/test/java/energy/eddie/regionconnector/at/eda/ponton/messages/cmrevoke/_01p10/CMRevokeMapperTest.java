// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p10;

import at.ebutilities.schemata.customerconsent.cmrevoke._01p10.CMRevoke;
import at.ebutilities.schemata.customerconsent.cmrevoke._01p10.ProcessDirectory;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization tests pinning the MapStruct mapping to the values the hand-written
 * {@code EdaCMRevoke01p10} wrapper record produced.
 */
class CMRevokeMapperTest {

    @Test
    void testMapping() {
        // Given
        var consentEnd = DatatypeFactory.newDefaultInstance()
                                        .newXMLGregorianCalendar(2027, 12, 31, 0, 0, 0, 0, 0);
        var cmRevoke = new CMRevoke()
                .withProcessDirectory(
                        new ProcessDirectory()
                                .withMeteringPoint("AT1111111111111111111111111111111")
                                .withConsentId("consent-1")
                                .withConsentEnd(consentEnd)
                );

        // When
        var res = CMRevokeMapper.INSTANCE.toEdaCMRevoke(cmRevoke);

        // Then
        assertEquals("AT1111111111111111111111111111111", res.meteringPoint());
        assertEquals("consent-1", res.consentId());
        assertEquals(LocalDate.of(2027, 12, 31), res.consentEnd());
    }
}
