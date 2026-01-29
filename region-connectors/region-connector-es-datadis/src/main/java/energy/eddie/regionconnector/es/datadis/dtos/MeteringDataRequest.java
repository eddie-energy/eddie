// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.time.LocalDate;

/**
 * Represents all the data needed to request metering data from Datadis.
 *
 * @param authorizedNif   NIF of the user for which the data is requested
 * @param meteringPoint   Metering point for which the data is requested (from Supply request)
 * @param distributorCode Distributor code for associated with the metering point (from Supply request)
 * @param startDate       Start date of the period for which the data is requested (inclusive, only year and month are
 *                        used)
 * @param endDate         End date of the period for which the data is requested (inclusive, only year and month are
 *                        used)
 * @param measurementType Type of measurement (hourly or quarter-hourly)
 * @param pointType       Type of metering point (from Supply request)
 */
public record MeteringDataRequest(String authorizedNif, String meteringPoint, String distributorCode,
                                  LocalDate startDate, LocalDate endDate, MeasurementType measurementType,
                                  String pointType) {
    public static MeteringDataRequest fromPermissionRequest(
            EsPermissionRequest permissionRequest,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new MeteringDataRequest(
                permissionRequest.nif(),
                permissionRequest.meteringPointId(),
                permissionRequest.distributorCode()
                                 .map(DistributorCode::getCode)
                                 .orElseThrow(() -> new IllegalStateException("")),
                startDate,
                endDate,
                permissionRequest.measurementType(),
                permissionRequest.pointType().map(String::valueOf).orElseThrow(() -> new IllegalStateException(""))
        );
    }

    public MeteringDataRequest minusMonths(int months) {
        return new MeteringDataRequest(
                authorizedNif,
                meteringPoint,
                distributorCode,
                startDate.minusMonths(months),
                endDate,
                measurementType,
                pointType
        );
    }
}