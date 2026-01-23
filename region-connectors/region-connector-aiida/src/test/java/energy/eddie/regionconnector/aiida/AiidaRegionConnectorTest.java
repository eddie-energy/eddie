// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.services.AiidaPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorTest {
    @Mock
    private AiidaPermissionService mockService;
    private AiidaRegionConnector connector;

    @BeforeEach
    void setUp() {
        connector = new AiidaRegionConnector(mockService);
    }

    @Test
    void getMetadata_MdaCodeIsAiida() {
        assertEquals("aiida", connector.getMetadata().id());
    }

    @Test
    void verify_terminate_callsMqtt() {
        // Given
        var permissionId = UUID.randomUUID().toString();

        // When
        connector.terminatePermission(permissionId);

        // Then
        verify(mockService).terminatePermission(permissionId);
    }
}
