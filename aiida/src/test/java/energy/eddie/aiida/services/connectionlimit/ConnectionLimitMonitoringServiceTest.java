// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitMonitoring;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.repositories.ConnectionLimitMonitoringRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitMonitoringServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");

    @Mock
    private ConnectionLimitMonitoringRepository connectionLimitMonitoringRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private DataSourceRepository dataSourceRepository;
    @Mock
    private AuthService authService;

    private ConnectionLimitMonitoringService service;

    @BeforeEach
    void setUp() {
        service = new ConnectionLimitMonitoringService(connectionLimitMonitoringRepository,
                                                       permissionRepository,
                                                       dataSourceRepository,
                                                       authService);
    }

    @Test
    void givenExistingAssignment_returnsAssignedDataSource() throws Exception {
        mockOwnedMonitorablePermission();
        when(connectionLimitMonitoringRepository.findById(PERMISSION_ID))
                .thenReturn(Optional.of(new ConnectionLimitMonitoring(PERMISSION_ID, DATA_SOURCE_ID)));

        var result = service.getConnectionLimitMonitoring(PERMISSION_ID);

        assertTrue(result.isPresent());
        assertEquals(PERMISSION_ID, result.get().permissionId());
        assertEquals(DATA_SOURCE_ID, result.get().dataSourceId());
    }

    @Test
    void givenNoAssignment_returnsEmpty() throws Exception {
        mockOwnedMonitorablePermission();
        when(connectionLimitMonitoringRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        assertTrue(service.getConnectionLimitMonitoring(PERMISSION_ID).isEmpty());
    }

    @Test
    void givenMissingPermission_throwsNotFound() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.getConnectionLimitMonitoring(PERMISSION_ID));
    }

    @Test
    void givenPermissionWithoutConnectionLimits_throwsNotAllowed() throws Exception {
        var permission = mock(Permission.class);
        when(permission.supportsConnectionLimits()).thenReturn(false);
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID))
                .thenReturn(Optional.of(permission));
        when(authService.getCurrentUserId()).thenReturn(USER_ID);

        assertThrows(ConnectionLimitMonitoringNotAllowedException.class,
                     () -> service.getConnectionLimitMonitoring(PERMISSION_ID));
    }

    @Test
    void givenValidRequest_assignsDataSource() throws Exception {
        mockOwnedMonitorablePermission();

        var dataSource = mock(DataSource.class);
        when(dataSource.type()).thenReturn(DataSourceType.SIMULATION);
        when(dataSourceRepository.findByIdAndUserId(DATA_SOURCE_ID, USER_ID)).thenReturn(Optional.of(dataSource));

        var result = service.updateConnectionLimitMonitoring(PERMISSION_ID, DATA_SOURCE_ID);

        assertEquals(DATA_SOURCE_ID, result.dataSourceId());

        var captor = ArgumentCaptor.forClass(ConnectionLimitMonitoring.class);
        verify(connectionLimitMonitoringRepository).save(captor.capture());
        assertEquals(PERMISSION_ID, captor.getValue().permissionId());
        assertEquals(DATA_SOURCE_ID, captor.getValue().dataSourceId());
    }

    @Test
    void givenMissingDataSource_throwsNotFound() throws Exception {
        mockOwnedMonitorablePermission();
        when(dataSourceRepository.findByIdAndUserId(DATA_SOURCE_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(DataSourceNotFoundException.class,
                     () -> service.updateConnectionLimitMonitoring(PERMISSION_ID, DATA_SOURCE_ID));
    }

    @Test
    void givenInboundDataSource_throwsNotAllowed() throws Exception {
        mockOwnedMonitorablePermission();
        var dataSource = mock(DataSource.class);
        when(dataSource.type()).thenReturn(DataSourceType.INBOUND);
        when(dataSourceRepository.findByIdAndUserId(DATA_SOURCE_ID, USER_ID)).thenReturn(Optional.of(dataSource));

        assertThrows(ConnectionLimitMonitoringNotAllowedException.class,
                     () -> service.updateConnectionLimitMonitoring(PERMISSION_ID, DATA_SOURCE_ID));
    }

    @Test
    void deleteConnectionLimitMonitoring_removesAssignment() throws Exception {
        mockOwnedMonitorablePermission();

        service.deleteConnectionLimitMonitoring(PERMISSION_ID);

        verify(connectionLimitMonitoringRepository).deleteById(PERMISSION_ID);
    }

    private void mockOwnedMonitorablePermission() throws Exception {
        var permission = mock(Permission.class);
        when(permission.supportsConnectionLimits()).thenReturn(true);
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID))
                .thenReturn(Optional.of(permission));
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
    }
}
