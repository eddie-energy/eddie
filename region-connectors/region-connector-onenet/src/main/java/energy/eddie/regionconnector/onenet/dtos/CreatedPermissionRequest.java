// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.dtos;

/**
 * This should hold all data that is required for the frontend to inform the final customer on how and where to accept or reject this permission request.
 * TODO: Extend with all the other required attributes to allow the final customer to give consent to share their data. This will probably not be required in this case
 *
 * @param permissionId the final permission ID
 */
public record CreatedPermissionRequest(String permissionId) {}
