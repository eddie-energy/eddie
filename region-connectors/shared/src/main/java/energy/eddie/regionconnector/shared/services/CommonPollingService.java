// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services;

public interface CommonPollingService<T> {

    /**
     * Polls future meter readings for active permission requests.
     * @param activePermission The permission request that data needs to be polled for.
     */
    void pollTimeSeriesData(T activePermission);

    /**
     * Checks, if a permission request is currently active and needs to fetch data.
     * @param permissionRequest The permission request whose status needs to be checked.
     * @return True if active and needs to be fetched, otherwise false.
     */
    boolean isActiveAndNeedsToBeFetched(T permissionRequest);
}
