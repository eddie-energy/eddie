package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.time.LocalDate;

/**
 * Represents all the data needed to request metering data from Datadis.
 */
public final class MeteringDataRequest {
    private final String authorizedNif;
    private final String meteringPoint;
    private final String distributorCode;
    private final LocalDate endDate;
    private final MeasurementType measurementType;
    private final String pointType;
    private LocalDate startDate;

    /**
     * @param authorizedNif   NIF of the user for which the data is requested
     * @param meteringPoint   Metering point for which the data is requested (from Supply request)
     * @param distributorCode Distributor code for associated with the metering point (from Supply request)
     * @param startDate       Start date of the period for which the data is requested (inclusive, only year and month are used)
     * @param endDate         End date of the period for which the data is requested (inclusive, only year and month are used)
     * @param measurementType Type of measurement (hourly or quarter-hourly)
     * @param pointType       Type of metering point (from Supply request)
     */
    public MeteringDataRequest(String authorizedNif, String meteringPoint, String distributorCode,
                               LocalDate startDate,
                               LocalDate endDate,
                               MeasurementType measurementType, String pointType) {
        this.authorizedNif = authorizedNif;
        this.meteringPoint = meteringPoint;
        this.distributorCode = distributorCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.measurementType = measurementType;
        this.pointType = pointType;
    }

    public static MeteringDataRequest fromPermissionRequest(EsPermissionRequest permissionRequest, LocalDate startDate, LocalDate endDate) {
        return new MeteringDataRequest(
                permissionRequest.nif(),
                permissionRequest.meteringPointId(),
                permissionRequest.distributorCode().map(DistributorCode::getCode).orElseThrow(() -> new IllegalStateException("")),
                startDate,
                endDate,
                permissionRequest.measurementType(),
                permissionRequest.pointType().map(String::valueOf).orElseThrow(() -> new IllegalStateException(""))
        );
    }

    public String authorizedNif() {
        return authorizedNif;
    }

    public String meteringPoint() {
        return meteringPoint;
    }

    public String distributorCode() {
        return distributorCode;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public MeasurementType measurementType() {
        return measurementType;
    }

    public String pointType() {
        return pointType;
    }

    public void minusMonths(int months) {
        startDate = startDate.minusMonths(months);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MeteringDataRequest) obj;
        return Objects.equals(this.authorizedNif, that.authorizedNif) &&
                Objects.equals(this.meteringPoint, that.meteringPoint) &&
                Objects.equals(this.distributorCode, that.distributorCode) &&
                Objects.equals(this.startDate, that.startDate) &&
                Objects.equals(this.endDate, that.endDate) &&
                Objects.equals(this.measurementType, that.measurementType) &&
                Objects.equals(this.pointType, that.pointType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizedNif, meteringPoint, distributorCode, startDate, endDate, measurementType, pointType);
    }

    @Override
    public String toString() {
        return "MeteringDataRequest[" +
                "authorizedNif=" + authorizedNif + ", " +
                "meteringPoint=" + meteringPoint + ", " +
                "distributorCode=" + distributorCode + ", " +
                "startDate=" + startDate + ", " +
                "endDate=" + endDate + ", " +
                "measurementType=" + measurementType + ", " +
                "pointType=" + pointType + ']';
    }
}