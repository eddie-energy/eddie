package energy.eddie.regionconnector.shared.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonPathsTest {

    @Test
    void testGetServletPathForRegionConnector_returnsPathForRegionConnector() {
        // Given
        var rc = "at-eda";

        // When
        var res = CommonPaths.getServletPathForRegionConnector(rc);

        // Then
        assertEquals("/region-connectors/at-eda/*", res);
    }

    @Test
    void testGetServletPathForOutboundConnector_returnsPathForOutboundConnector() {
        // Given
        var rc = "admin-console";

        // When
        var res = CommonPaths.getServletPathForOutboundConnector(rc);

        // Then
        assertEquals("/outbound-connectors/admin-console/*", res);
    }

    @Test
    void testGetClassPathForCeElement_returnsPathForCeElement() {
        // Given
        var rc = "at-eda";

        // When
        var res = CommonPaths.getClasspathForCeElement(rc);

        // Then
        assertEquals("/public/region-connectors/at-eda/ce.js", res);
    }
}