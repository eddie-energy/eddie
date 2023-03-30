package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.EnergyDirection;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.ReqType;
import at.ebutilities.schemata.customerconsent.cmrequest._01p10.TransmissionCycle;
import at.eda.xml.builders.helper.DateTimeConverter;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * <p>Allows to create ReqType Object.
 * <p>Only <b>reqDatType</b> and <b>dateFrom</b> attributes are necessarily required according to the schema definition.
 *
 * @see ReqType
 */

public class ReqTypeBuilder {
    private String reqDatType = "";
    @Nullable
    private LocalDate dateFrom;
    @Nullable
    private LocalDate dateTo;
    @Nullable
    private MeteringIntervallType meteringIntervall;
    @Nullable
    private TransmissionCycle transmissionCycle;
    @Nullable
    private String ecid;
    @Nullable
    private BigDecimal ecShare;
    @Nullable
    private EnergyDirection energyDirection;

    /**
     * Sets the datatype of the request (<a href="https://www.ebutilities.at/documents/20220309103941_datentypen.pdf">CCM-Datatypes</a>)
     *
     * @param reqDatType allowed object is
     *                   {@link String} max. 30 characters
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withReqDatType(String reqDatType) {
        if (reqDatType == null || reqDatType.length() == 0) {
            throw new IllegalArgumentException("`reqDatType` cannot be empty.");
        }

        int LEN_REQ_DAT_TYPE = 30;
        if (reqDatType.length() > LEN_REQ_DAT_TYPE) {
            throw new IllegalArgumentException("`reqDatType` length cannot exceed " + LEN_REQ_DAT_TYPE + " characters.");
        }

        this.reqDatType = reqDatType;
        return this;
    }

    /**
     * Sets the start date of data transmission permission
     *
     * @param dateFrom allowed object is
     *                 {@link LocalDate}
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withDateFrom(LocalDate dateFrom) {
        if (dateFrom == null) {
            throw new IllegalArgumentException("`dateFrom` cannot be empty.");
        }

        this.dateFrom = dateFrom;
        return this;
    }

    /**
     * Sets the end date of data transmission permission
     *
     * @param dateTo allowed object is
     *               {@link LocalDate}
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withDateTo(@Nullable LocalDate dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    /**
     * Sets the granularity of the metered data
     *
     * @param meteringIntervall allowed object is
     *                          {@link MeteringIntervallType}
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withMeteringIntervall(@Nullable MeteringIntervallType meteringIntervall) {
        this.meteringIntervall = meteringIntervall;
        return this;
    }

    /**
     * Sets the transmission interval consumption data
     *
     * @param transmissionCycle allowed object is
     *                          {@link TransmissionCycle}
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withTransmissionCycle(@Nullable TransmissionCycle transmissionCycle) {
        this.transmissionCycle = transmissionCycle;
        return this;
    }

    /**
     * Sets the identifier of an energy community (technical community ID)
     *
     * @param ecid allowed object is
     *             {@link String} max. length 33
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withEcId(String ecid) {
        int LEN_ECID = 33;
        if (ecid != null && ecid.length() > LEN_ECID) {
            throw new IllegalArgumentException("`ecid` length cannot exceed " + LEN_ECID + " characters.");
        }

        this.ecid = ecid;
        return this;
    }

    /**
     * Sets the share in static model of generation communities
     *
     * @param ecShare allowed object is
     *                {@link BigDecimal} with 4 decimal places
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withEcShare(@Nullable BigDecimal ecShare) {
        this.ecShare = ecShare != null ? ecShare.setScale(4, RoundingMode.HALF_EVEN) : null;
        return this;
    }

    /**
     * Sets the energy direction to distinguish between consumers and feeders in generation communities
     *
     * @param energyDirection allowed object is
     *                        {@link EnergyDirection}
     * @return {@link ReqTypeBuilder}
     */
    public ReqTypeBuilder withEnergyDirection(@Nullable EnergyDirection energyDirection) {
        this.energyDirection = energyDirection;
        return this;
    }

    /**
     * Creates and returns a ReqType Object
     *
     * @return {@link ReqType}
     */
    public ReqType build() {
        if (reqDatType.length() == 0 || dateFrom == null) {
            throw new IllegalStateException("Attributes `reqDatType` and `dateFrom` are required.");
        }

        ReqType reqType = new ReqType();
        reqType.setReqDatType(reqDatType);
        reqType.setDateFrom(DateTimeConverter.dateToXMl(dateFrom));

        if (dateTo != null) {
            if (dateFrom.isAfter(dateTo)) {
                throw new IllegalStateException("Attribute `dateFrom`(" + dateFrom + ") is after `dateTo`(" + dateTo + ").");
            } else {
                reqType.setDateFrom(DateTimeConverter.dateToXMl(dateTo));
            }
        }

        reqType.setMeteringIntervall(meteringIntervall);
        reqType.setTransmissionCycle(transmissionCycle);
        reqType.setECID(ecid);
        reqType.setECShare(ecShare);
        reqType.setEnergyDirection(energyDirection);

        return reqType;
    }
}
