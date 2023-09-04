package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

/**
 * This class represents the supply data returned by the Datadis API.
 * It contains information about supplies (metering points) associated to a NIF.
 */
public class Supply {

    @Nullable
    private String address;

    @Nullable
    @JsonProperty("cups")
    private String meteringPoint;

    @Nullable
    private String postalCode;

    @Nullable
    private String province;

    @Nullable
    private String municipality;

    @Nullable
    private String distributor;

    @Nullable
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate validDateFrom;

    @Nullable
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate validDateTo;

    @Nullable
    private Integer pointType;

    @Nullable
    private String distributorCode;

    @Nullable
    public String getAddress() {
        return address;
    }

    public void setAddress(@Nullable String address) {
        this.address = address;
    }

    @Nullable
    public String getMeteringPoint() {
        return meteringPoint;
    }

    public void setMeteringPoint(@Nullable String meteringPoint) {
        this.meteringPoint = meteringPoint;
    }

    @Nullable
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(@Nullable String postalCode) {
        this.postalCode = postalCode;
    }

    @Nullable
    public String getProvince() {
        return province;
    }

    public void setProvince(@Nullable String province) {
        this.province = province;
    }

    @Nullable
    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(@Nullable String municipality) {
        this.municipality = municipality;
    }

    @Nullable
    public String getDistributor() {
        return distributor;
    }

    public void setDistributor(@Nullable String distributor) {
        this.distributor = distributor;
    }

    @Nullable
    public LocalDate getValidDateFrom() {
        return validDateFrom;
    }

    public void setValidDateFrom(@Nullable LocalDate validDateFrom) {
        this.validDateFrom = validDateFrom;
    }

    @Nullable
    public LocalDate getValidDateTo() {
        return validDateTo;
    }

    public void setValidDateTo(@Nullable LocalDate validDateTo) {
        this.validDateTo = validDateTo;
    }

    @Nullable
    public Integer getPointType() {
        return pointType;
    }

    public void setPointType(@Nullable Integer pointType) {
        this.pointType = pointType;
    }

    @Nullable
    public String getDistributorCode() {
        return distributorCode;
    }

    public void setDistributorCode(@Nullable String distributorCode) {
        this.distributorCode = distributorCode;
    }

    @Override
    public String toString() {
        return "Supply{" +
                "address='" + address + '\'' +
                ", meteringPoint='" + meteringPoint + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", province='" + province + '\'' +
                ", municipality='" + municipality + '\'' +
                ", distributor='" + distributor + '\'' +
                ", validDateFrom=" + validDateFrom +
                ", validDateTo=" + validDateTo +
                ", pointType=" + pointType +
                ", distributorCode='" + distributorCode + '\'' +
                '}';
    }
}
