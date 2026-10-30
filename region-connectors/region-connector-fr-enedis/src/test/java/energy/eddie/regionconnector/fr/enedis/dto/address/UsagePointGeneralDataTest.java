// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.address;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UsagePointGeneralDataTest {

    @Test
    void deserializeGeneralData_parsesInstallationAddress() throws IOException {
        var data = TestResourceProvider.readFromFile("donnees-generales.json", UsagePointGeneralData.class);

        assertNotNull(data.address());
        var installation = data.address().address();
        assertNotNull(installation);
        assertEquals("75112", installation.inseeCode());
        assertEquals("75000 Paris", installation.postalCodeCity());
        assertEquals("12 rue de la Paix", installation.numberStreetName());
        assertEquals("Les Prés", installation.locality());
    }
}
