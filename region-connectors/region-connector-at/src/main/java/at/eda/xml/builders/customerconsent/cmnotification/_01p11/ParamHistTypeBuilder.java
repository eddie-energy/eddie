package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.MeteringIntervallType;
import at.ebutilities.schemata.customerconsent.cmnotification._01p11.ParamHistType;
import at.eda.xml.builders.helper.DateTimeConverter;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.Objects;

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
        this.dateFrom = Objects.requireNonNull(dateFrom, "`dateFrom` cannot be empty.");
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
        this.dateTo = Objects.requireNonNull(dateTo, "`dateTo` cannot be empty.");
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
        this.meteringIntervall = Objects.requireNonNull(meteringIntervall, "`meteringIntervall` cannot be empty.");
        return this;
    }

    /**
     * Creates and returns a ParamHistType Object
     *
     * @return {@link ParamHistType}
     */
    public ParamHistType build() {
        if (Objects.requireNonNull(dateFrom, "Attribute `dateFrom` is required.").isAfter(Objects.requireNonNull(dateTo, "Attribute `dateFrom` is required."))) {
            throw new NullPointerException("Attribute `dateFrom`(" + dateFrom + ") is after `dateTo`(" + dateTo + ").");
        }

        ParamHistType paramHist = new ParamHistType();

        paramHist.setDateFrom(DateTimeConverter.dateToXMl(dateFrom));
        paramHist.setDateTo(DateTimeConverter.dateToXMl(dateTo));
        paramHist.setMeteringIntervall(Objects.requireNonNull(meteringIntervall, "Attribute `meteringIntervall` is required."));

        return paramHist;
    }
}
