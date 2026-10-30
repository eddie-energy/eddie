// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContractualSituationTest {

    @Test
    void deserializeArray_parsesRealResponseShape() throws IOException {
        var situations = readSituations("situation-contractuelle.json");

        assertEquals(1, situations.size());
        var situation = situations.getFirst();
        assertEquals("3127069600", situation.usagePointId());
        assertEquals("Contrat Protocole501", situation.contractType());
        assertEquals("ELECTRICITE DE FRANCE", situation.contractor());
        assertEquals("Tarif BT<=36kVA Courte Utilisation heures pleines heures creuses associées à deux saisons",
                     situation.distributionTariff());
        // segment is returned as a scalar, subscribed_power as a {unit, value} object
        assertEquals(List.of("C5"), situation.segments());
        assertEquals("6", situation.subscribedPower());
        // the customer address is doubly nested under the literal "adress" key
        assertNotNull(situation.customer());
        assertNotNull(situation.customer().customer());
        assertNotNull(situation.customer().customer().adress());
        assertEquals("M VIGNAL ANDRE", situation.customer().customer().adress().line1());
        assertEquals("rue DU CENTRE", situation.customer().customer().adress().line4());
        assertEquals("34210 AIGUES VIVES", situation.customer().customer().adress().line6());
        // contact data and person are element-root siblings of customer
        assertNotNull(situation.contactData());
        assertEquals("0000000000", situation.contactData().landline());
        assertNotNull(situation.person());
        assertEquals("M", situation.person().title());
        assertEquals("VIGNAL", situation.person().lastName());
        assertEquals("ANDRE", situation.person().firstName());
        assertNull(situation.organization());
    }

    @Test
    void deserializeArray_withoutCustomer_toleratesNulls() throws IOException {
        var situations = readSituations("situation-contractuelle-no-customer.json");

        var situation = situations.getFirst();
        assertNull(situation.customer());
        assertNull(situation.contactData());
        assertNull(situation.person());
        assertEquals(List.of("P4"), situation.segments());
        assertNull(situation.subscribedPower());
    }

    private static List<ContractualSituation> readSituations(String resource) throws IOException {
        var array = TestResourceProvider.readFromFile(resource, ContractualSituation[].class);
        return Arrays.asList(array);
    }
}