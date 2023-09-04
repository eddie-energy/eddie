package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * This class represents the metering data returned by the Datadis API.
 * It contains both consumption and surplus energy (production) data.
 */
public class MeteringData {

    @Nullable
    private String cups;

    @Nullable
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate date;

    @Nullable
    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    @Nullable
    private Double consumptionKWh;

    @Nullable
    private String obtainMethod;

    @Nullable
    private Double surplusEnergyKWh;

    @Nullable
    public String getCups() {
        return cups;
    }

    public void setCups(@Nullable String cups) {
        this.cups = cups;
    }

    @Nullable
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@Nullable LocalDate date) {
        this.date = date;
    }

    @Nullable
    public LocalTime getTime() {
        return time;
    }

    public void setTime(@Nullable LocalTime time) {
        this.time = time;
    }

    @Nullable
    public Double getConsumptionKWh() {
        return consumptionKWh;
    }

    public void setConsumptionKWh(@Nullable Double consumptionKWh) {
        this.consumptionKWh = consumptionKWh;
    }

    @Nullable
    public String getObtainMethod() {
        return obtainMethod;
    }

    public void setObtainMethod(@Nullable String obtainMethod) {
        this.obtainMethod = obtainMethod;
    }

    @Nullable
    public Double getSurplusEnergyKWh() {
        return surplusEnergyKWh;
    }

    public void setSurplusEnergyKWh(@Nullable Double surplusEnergyKWh) {
        this.surplusEnergyKWh = surplusEnergyKWh;
    }
}
