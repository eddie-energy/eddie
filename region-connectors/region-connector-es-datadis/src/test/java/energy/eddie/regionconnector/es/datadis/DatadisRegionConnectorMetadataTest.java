// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DatadisRegionConnectorMetadataTest {

    @Test
    void getInstance() {
        var instance1 = DatadisRegionConnectorMetadata.getInstance();
        var instance2 = DatadisRegionConnectorMetadata.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void id() {
        assertEquals("es-datadis", DatadisRegionConnectorMetadata.getInstance().id());
    }

    @Test
    void countryCode() {
        assertEquals("ES", DatadisRegionConnectorMetadata.getInstance().countryCode());
    }

    @Test
    void coveredMeteringPoints() {
        assertEquals(30234170, DatadisRegionConnectorMetadata.getInstance().coveredMeteringPoints());
    }
}