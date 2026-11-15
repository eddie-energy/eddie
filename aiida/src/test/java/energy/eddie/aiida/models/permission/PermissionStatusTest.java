// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionStatusTest {
    @Test
    void isAccepted_accepted_returnsTrue() {
        assertThat(PermissionStatus.ACCEPTED.isAccepted()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PermissionStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    void isAccepted_finalStatus_returnsFalse(PermissionStatus status) {
        assertThat(status.isAccepted()).isFalse();
    }
}
