// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos;

public enum PatchOperation {
    /**
     * Use this patch operation to indicate that the customer wants to revoke the specific permission.
     */
    REVOKE,
    ACCEPT,
    REJECT
}
