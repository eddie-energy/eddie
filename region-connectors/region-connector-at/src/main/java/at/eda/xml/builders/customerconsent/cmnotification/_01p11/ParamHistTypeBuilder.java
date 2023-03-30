package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ParamHistType;
import at.eda.xml.builders.helper.DateTimeConverter;

import javax.annotation.Nullable;
import java.time.LocalDate;

/**
 * <p>Allows to create a ParamHistType Object for CMNotification.
 * <p>All fields are required according to the schema definition.
 *
 * @see ParamHistType
 */
public class ParamHistTypeBuilder {
    @Nullable
    private LocalDate dateFrom;
    @Nullable
    private LocalDate dateTo;
    @Nullable
    private MeteringIntervallType meteringIntervall;

    /**
     * Sets the start date of data access
     *
     * @param dateFrom allowed object is
     *                 {@link LocalDate}
     * @return {@link ParamHistTypeBuilder}
     */
    public ParamHistTypeBuilder withDateFrom(LocalDate dateFrom) {
        if (dateFrom == null) {
            throw new IllegalArgumentException("`dateFrom` cannot be empty.");
        }

        this.dateFrom = dateFrom;
        return this;
    }

    /**
     * Sets the end date of data access
     *
     * @param dateTo allowed object is
     *               {@link LocalDate}
     * @return {@link ParamHistTypeBuilder}
     */
    public ParamHistTypeBuilder withDateTo(LocalDate dateTo) {
        if (dateTo == null) {
            throw new IllegalArgumentException("`dateTo` cannot be empty.");
        }

        this.dateTo = dateTo;
        return this;
    }

    /**
     * Sets the granularity of the data
     *
     * @param meteringIntervall allowed object is
     *                          {@link MeteringIntervallType}
     * @return {@link ParamHistTypeBuilder}
     */
    public ParamHistTypeBuilder withMeteringIntervall(MeteringIntervallType meteringIntervall) {
        if (meteringIntervall == null) {
            throw new IllegalArgumentException("`meteringIntervall` cannot be empty.");
        }

        this.meteringIntervall = meteringIntervall;
        return this;
    }

    /**
     * Creates and returns a ParamHistType Object
     *
     * @return {@link ParamHistType}
     */
    public ParamHistType build() {
        if (dateFrom == null || dateTo == null || meteringIntervall == null) {
            throw new IllegalStateException("Attributes `dateFrom`, `dateTo` and `meteringIntervall` are required.");
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new IllegalStateException("Attribute `dateFrom`(" + dateFrom + ") is after `dateTo`(" + dateTo + ").");
        }

        ParamHistType paramHist = new ParamHistType();

        paramHist.setDateFrom(DateTimeConverter.dateToXMl(dateFrom));
        paramHist.setDateTo(DateTimeConverter.dateToXMl(dateTo));
        paramHist.setMeteringIntervall(meteringIntervall);

        return paramHist;
    }
}
