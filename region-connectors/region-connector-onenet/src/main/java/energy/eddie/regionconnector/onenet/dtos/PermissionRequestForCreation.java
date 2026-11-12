// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet.dtos;

/**
 * This should hold all information necessary to create a permission request and send it to the permission administrator.
 * TODO: Extend with all the other IDs to identify a consent with the validated historical data received from onenet
 *
 * @param connectionId the connection ID
 * @param dataNeedId   the data need ID
 */
public record PermissionRequestForCreation(String connectionId, String dataNeedId) {}
