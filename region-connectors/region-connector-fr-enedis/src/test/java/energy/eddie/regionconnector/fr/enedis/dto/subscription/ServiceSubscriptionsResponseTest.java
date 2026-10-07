// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("DataFlowIssue")
class ServiceSubscriptionsResponseTest {

    @Test
    void deserializeResponse_withActiveService_parsesAllValues() throws IOException {
        // Given
        var response = TestResourceProvider.readFromFile("subscribed-services-response.json",
                                                         ServiceSubscriptionsResponse.class);

        // Then
        assertEquals(2L, response.totalServices());
        assertEquals(2, response.services().size());

        var first = response.services().getFirst();
        assertEquals("1", first.id());
        assertFalse(first.injection());
        assertTrue(first.consumption());
        assertEquals(ServiceType.ACCES, first.serviceType());
        assertEquals(LocalDate.of(2026, 1, 15), first.start());
        assertEquals(LocalDate.of(2029, 1, 15), first.end());
        assertEquals(ServiceState.ACTIVE, first.state());
        assertEquals("Actif", first.stateLabel());
        assertEquals("12345678901234", first.pointId());
        assertEquals("123456789", first.holderSiren());
        assertEquals("987654321", first.beneficiarySiren());
        assertEquals(MeasureType.LOAD_CURVE, first.measureType());
        assertEquals("PT30M", first.measureStep());
        assertEquals("P1D", first.transmissionPeriodicity());
        assertTrue(first.correctedMeasures());
        assertFalse(first.dynamicSpace());
        assertFalse(first.dataPublication());
    }

    @Test
    void deserializeResponse_withActiveService_parsesAuthorization() throws IOException {
        // Given
        var response = TestResourceProvider.readFromFile("subscribed-services-response.json",
                                                         ServiceSubscriptionsResponse.class);

        // Then
        var authorization = response.services().getFirst().authorization();
        assertNotNull(authorization);
        assertEquals(999999999L, authorization.id());
        assertEquals("Consentement client", authorization.label());
        assertEquals(AuthorizationType.EXPLICIT, authorization.type());
    }

    @Test
    void deserializeResponse_withRequestedService_mapsStateAndMeasureType() throws IOException {
        // Given
        var response = TestResourceProvider.readFromFile("subscribed-services-response.json",
                                                         ServiceSubscriptionsResponse.class);

        // Then
        var second = response.services().get(1);
        assertEquals(ServiceState.REQUESTED, second.state());
        assertEquals("Demande", second.stateLabel());
        assertEquals(MeasureType.INDEX, second.measureType());
        assertNotNull(second.authorization());
        assertEquals(AuthorizationType.IMPLICIT, second.authorization().type());
    }
}
