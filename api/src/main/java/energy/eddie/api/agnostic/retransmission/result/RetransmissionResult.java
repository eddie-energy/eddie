// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public sealed interface RetransmissionResult permits
        Success,
        PermissionRequestNotFound,
        NotSupported,
        NoPermissionForTimeFrame,
        NoActivePermission,
        DataNotAvailable,
        RetransmissionServiceNotFound,
        Failure {

    /**
     * The permission id the original request was for.
     *
     * @return the permission id
     */
    String permissionId();

    /**
     * The timestamp when the result was created.
     *
     * @return the timestamp of the result
     */
    ZonedDateTime timestamp();
}
