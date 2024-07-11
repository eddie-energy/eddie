package energy.eddie.api.v0;

import energy.eddie.cim.v0_82.cmd.StatusTypeList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PermissionProcessStatusTest {

    @ParameterizedTest
    @EnumSource(PermissionProcessStatus.class)
    void permissionProcessStatus_hasSameConstantsAsStatusTypeList(PermissionProcessStatus status) {
        // Given
        // When
        // Then
        assertNotNull(StatusTypeList.fromValue(status.name()));
    }
}