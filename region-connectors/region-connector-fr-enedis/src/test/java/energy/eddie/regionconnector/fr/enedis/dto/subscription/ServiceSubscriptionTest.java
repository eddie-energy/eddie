// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import energy.eddie.regionconnector.fr.enedis.TestResourceProvider;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("DataFlowIssue")
class ServiceSubscriptionTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void deserializeRequest_withAllFields_parsesValues() throws IOException {
        // Given
        var request = TestResourceProvider.readFromFile("subscribed-services-request.json",
                                                        ServiceSubscription.class);

        // Then
        assertEquals(List.of("12345678901234"), request.pointId());
        assertEquals(List.of("123456789"), request.siren());
        assertEquals(LocalDate.of(2026, 1, 1), request.start());
        assertEquals(LocalDate.of(2026, 12, 31), request.end());
        assertEquals(List.of(ServiceState.ACTIVE, ServiceState.REQUESTED), request.serviceStates());
        assertEquals(ServiceType.ACCES, request.serviceType());
        assertEquals(List.of(MeasureType.LOAD_CURVE, MeasureType.INDEX), request.measureTypes());
        assertTrue(request.consumption());
        assertFalse(request.injection());
        assertEquals(0, request.page());
        assertFalse(request.count());
        assertEquals(9999999999L, request.authorizationId());
        assertTrue(request.authorization());
    }

    @Test
    void deserializeRequest_withMinimalBody_defaultsAndNullsAreTolerated() {
        // Given
        var request = OBJECT_MAPPER.readValue("""
                                                      {"comptage": false}
                                                      """, ServiceSubscription.class);

        // Then
        assertFalse(request.count());
        assertNotNull(request);
    }

    @Test
    void serializeRequest_withConvenienceConstructor_writesQueryForDataConnectFlow() {
        // Given
        var request = new ServiceSubscription(123456789L);

        // When
        String json = OBJECT_MAPPER.writeValueAsString(request);

        // Then
        assertTrue(json.contains("\"serviceType\":\"ACCES\""));
        assertTrue(json.contains("\"comptage\":false"));
        assertTrue(json.contains("\"autorisation\":true"));
        assertTrue(json.contains("\"autorisationId\":123456789"));
        assertFalse(json.contains("\"pointId\""));
        assertFalse(json.contains("\"siren\""));
    }
}
