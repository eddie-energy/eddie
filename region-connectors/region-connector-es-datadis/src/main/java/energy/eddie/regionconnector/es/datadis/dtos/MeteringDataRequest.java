package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;

import java.time.LocalDate;

/**
 * Represents all the data needed to request metering data from Datadis.
 *
 * @param authorizedNif   NIF of the user for which the data is requested
 * @param meteringPoint   Metering point for which the data is requested (from Supply request)
 * @param distributorCode Distributor code for associated with the metering point (from Supply request)
 * @param startDate       Start date of the period for which the data is requested (inclusive, only year and month are used)
 * @param endDate         End date of the period for which the data is requested (inclusive, only year and month are used)
 * @param measurementType Type of measurement (hourly or quarter-hourly)
 * @param pointType       Type of metering point (from Supply request)
 */
public record MeteringDataRequest(String authorizedNif, String meteringPoint, String distributorCode,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  MeasurementType measurementType, String pointType) {
}